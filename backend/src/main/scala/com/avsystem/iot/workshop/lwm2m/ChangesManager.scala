package com.avsystem.iot.workshop
package lwm2m

import java.{util => ju}

import org.eclipse.leshan.core.node._
import org.eclipse.leshan.core.observation.{Observation, ObservationListener}
import org.eclipse.leshan.server.observation.ObservationRegistryListener
import com.avsystem.commons.jiop.BasicJavaInterop._
import com.avsystem.commons.misc.Opt
import com.avsystem.iot.workshop.lwm2m.ChangesManager.Listener
import org.eclipse.leshan.server.LwM2mServer
import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

object ChangesManager {

  trait Listener {
    def onUpdate(update: Vector[NodeUpdate]): Unit

    def endpointName: String
  }

}

class ChangesManager(private val server: LwM2mServer) extends ObservationRegistryListener {

  private val Logger = LoggerFactory.getLogger(getClass)

  private val lock = new AnyRef
  private val listeners: JHashMap[String, JList[Listener]] = new JHashMap()

  server.getObservationRegistry.addListener(this)

  def listenForChanges(listener: Listener): Unit = {
    lock.synchronized {
      val listenersListOpt = listeners.get(listener.endpointName).opt
      val list = listenersListOpt match {
        case Opt(l) => l
        case Opt.Empty =>
          val newList = new JArrayList[Listener]()
          listeners.put(listener.endpointName, newList)
          newList
      }
      if (!list.contains(listener)) {
        list.add(listener)
        Logger.debug(s"Listener added: $listener")
      } else {
        Logger.debug(s"Listener already added: $listener")
      }
    }
  }

  def cancelListener(listener: Listener): Unit = {
    lock.synchronized {
      listeners.get(listener.endpointName).opt.foreach(_.remove(listener))
      if (listeners.get(listener.endpointName).opt.map(_.isEmpty).getOrElse(false)) {
        listeners.remove(listener.endpointName)
      }
    }
  }

  override def newObservation(observation: Observation): Unit = {
    lock.synchronized {
      observationChanged(observation, value = true)
    }
  }

  override def cancelled(observation: Observation): Unit = {
    lock.synchronized {
      observationChanged(observation, value = false)
    }
  }

  override def newValue(observation: Observation, value: LwM2mNode): Unit = {
    lock.synchronized {
      Logger.debug(s"new value: ${observation.getPath} = $value")
      withListeners(observation) { listeners =>
        val obsPath = observation.getPath
        val update: Vector[NodeUpdate] = value match {
          case resource: LwM2mSingleResource => if (obsPath.isResource) {
            Vector(NodeUpdate(obsPath.toString, Opt.Empty, resource.getValue.toString.opt))
          } else if (obsPath.isObjectInstance) {
            Vector(NodeUpdate(new LwM2mPath(obsPath.getObjectId, obsPath.getObjectInstanceId, resource.getId).toString,
              Opt.Empty,
              resource.getValue.toString.opt))
          } else {
            Vector.empty
          }
          case objInstance: LwM2mObjectInstance =>
            objInstance.getResources.asScala.values.map { resource =>
              NodeUpdate(new LwM2mPath(obsPath.getObjectId, objInstance.getId, resource.getId).toString, Opt.Empty,
                resource.getValue.toString.opt)
            }.toVector
          case obj: LwM2mObject =>
            obj.getInstances.asScala.values.flatMap { objInstance =>
              objInstance.getResources.asScala.values.map { resource =>
                NodeUpdate(new LwM2mPath(obj.getId, objInstance.getId, resource.getId).toString, Opt.Empty,
                  resource.getValue.toString.opt)
              }
            }.toVector
        }
        if (update.nonEmpty) {
          notifyListeners(listeners, update)
        }
      }
    }
  }

  private def observationChanged(observation: Observation, value: Boolean): Unit = {
    Logger.debug(s"Observation changed: ${observation.getPath}, $value")
    withListeners(observation) { listeners =>
      val update = NodeUpdate(observation.getPath.toString, value.opt, Opt.Empty)
      notifyListeners(listeners, Vector(update))
    }
  }

  private def withListeners(observation: Observation)(onListeners: Iterable[Listener] => Unit): Unit = {
    val client = server.getClientRegistry.findByRegistrationId(observation.getRegistrationId)
    listeners.get(client.getEndpoint).opt.foreach { listeners =>
      onListeners(listeners.asScala)
    }
  }

  private def notifyListeners(listeners: Iterable[Listener], update: Vector[NodeUpdate]): Unit = {
    val buff = List.newBuilder[Listener]
    listeners.foreach { listener =>
      if (!notifyListener(update, listener)) {
        buff.+=(listener)
      }
    }
    val toRemoveListeners: List[Listener] = buff.result()
    toRemoveListeners.foreach(cancelListener(_))
    if (toRemoveListeners.nonEmpty) {
      Logger.warn(s"Listeners removed: $toRemoveListeners")
    }
  }

  private def notifyListener(update: Vector[NodeUpdate], listener: Listener): Boolean = {
    try {
      listener.onUpdate(update)
      true
    } catch {
      case NonFatal(e) =>
        Logger.error(s"Failed to notify listener: $listener, ${e.getMessage}")
        false
    }
  }
}


