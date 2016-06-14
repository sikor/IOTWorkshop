package com.avsystem.iot.workshop
package lwm2m

import java.nio.charset.Charset
import java.util.Date
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import com.avsystem.commons.concurrent.RunInQueueEC
import com.avsystem.commons.jiop.BasicJavaInterop._
import com.avsystem.commons.misc.Opt
import com.avsystem.iot.workshop.lwm2m.ChangesManager.Listener
import com.avsystem.iot.workshop.lwm2m.LmPathUtils.LmPath
import com.avsystem.iot.workshop.lwm2m.Lwm2mService._
import org.eclipse.leshan.core.node.{LwM2mNode, LwM2mObject, LwM2mObjectInstance, LwM2mSingleResource}
import org.eclipse.leshan.core.request._
import org.eclipse.leshan.core.response._
import org.eclipse.leshan.server.californium.LeshanServerBuilder
import org.eclipse.leshan.server.californium.impl.LeshanServer
import org.eclipse.leshan.server.client.Client
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.concurrent.{Future, Promise}

object Lwm2mService {

  private val Logger = LoggerFactory.getLogger(Lwm2mService.getClass)

  implicit class LeshanClientOps(private val client: Client) extends AnyVal {

    def toRegisteredClient: RegisteredClient = {
      RegisteredClient(client.getRegistrationDate, client.getAddress.toString, client.getPort, client.getLifeTimeInSec,
        client.getEndpoint, client.getRegistrationId, client.getAdditionalRegistrationAttributes.asScala.toMap,
        client.getRootPath, client.getLastUpdate, client.getObjectLinks.map(_.getUrl).toVector)
    }
  }

  implicit val ec = RunInQueueEC

  implicit class LeshanServerOps(private val server: LeshanServer) extends AnyVal {

    def sendFut[R <: LwM2mResponse](destination: Client, request: DownlinkRequest[R]): Future[R] = {
      val result = Promise[R]()
      server.send(destination, request, new ResponseCallback[R] {
        override def onResponse(response: R): Unit = result.success(response)
      }, new ErrorCallback {
        override def onError(e: Exception): Unit = result.failure(e)
      })
      result.future
    }
  }

}

class Lwm2mService(val bindHostname: String, val bindPort: Int) {

  val objectsSpec = new ObjectsSpec(Vector("/oma-objects-spec.json", "/custom-objects-spec.json"))
  val builder: LeshanServerBuilder = new LeshanServerBuilder
  builder.setLocalAddress(bindHostname, bindPort)
  val server: LeshanServer = builder.build()
  server.start()
  val changesManager = new ChangesManager(server)
  Logger.info(s"Lwm2m Service started: $this")

  private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1);

  def retrieveAllRegisteredClients(): Vector[RegisteredClient] = {
    Logger.debug("retrieving all registered clients")
    server.getClientRegistry.allClients().asScala.map(_.toRegisteredClient).toVector
  }

  def retrieveClientData(endpointName: String): Future[ClientData] = {
    withClient(endpointName) { client: Client =>
      val observations = server.getObservationRegistry.getObservations(client)
      val observationsMap = observations.asScala.map(ob => (ob.getPath.toString, ob)).toMap
      val dataForObjects: Array[Future[Vector[Lwm2mDMNode]]] = client.getObjectLinks.map { linkObject =>
        val url = linkObject.getUrl
        if (url.length > 1) {
          val pattern = "\\d+".r
          val objId = pattern.findFirstIn(url).get.toInt
          val objUrl: String = s"/$objId"
          for {
            discoverResponse <- server.sendFut(client, new DiscoverRequest(objUrl))
            discoveredUrlsMap = discoverResponse.getObjectLinks.map(ol => (ol.getUrl, Lwm2mDMNode(ol.getUrl, "",
              ol.getAttributes.asScala.mapValues(_.toString).toMap, objectsSpec.getDetails(ol.getUrl.lmPath), observationsMap.contains(ol.getUrl)))).toMap
            _ = Logger.trace("Discover urls: " + discoveredUrlsMap.values.toString)
            readValueResponse <- server.sendFut(client, new ReadRequest(objUrl))
            updatedMap = readValueResponse.getContent match {
              case obj: LwM2mObject =>
                obj.getInstances.asScala.values.foldLeft(discoveredUrlsMap) { (map, objInstace) =>
                  handleObjectInstance(obj.getId, map, objInstace)
                }
              case objInstance: LwM2mObjectInstance =>
                handleObjectInstance(objId, discoveredUrlsMap, objInstance)
              case _ => throw new IllegalStateException(s"Unexpected content: ${readValueResponse.getContent}")
            }
          } yield {
            updatedMap.values.toVector
          }
        } else {
          Future.successful(Vector.empty)
        }
      }
      val datamodelFut: Future[Vector[Lwm2mDMNode]] =
        dataForObjects.foldLeft(Future.successful(Vector.empty[Lwm2mDMNode])) { (allNodesFut, nodesPart) =>
          allNodesFut.flatMap { allNodes =>
            nodesPart.map { part =>
              allNodes ++ part
            }
          }
        }
      datamodelFut.map(dm => ClientData(client.toRegisteredClient, dm.sortBy(n => n.path)))
    }
  }

  def write(endpointName: String, path: LmPath, value: String): Future[Unit] = {
    withClient(endpointName) { client: Client =>
      val request = new WriteRequest(WriteRequest.Mode.REPLACE, ContentFormat.TLV, path.toUrl, toLwm2mNode(path, value))
      Logger.debug(s"Write: ($endpointName, $path, $value)")
      server.sendFut(client, request).filter(_.isSuccess).toUnit
    }
  }

  def observe(endpointName: String, path: LmPath): Future[Unit] = {
    withClient(endpointName) { client: Client =>
      val request = new ObserveRequest(path.toUrl)
      Logger.debug(s"Observe: ${path.toUrl}")
      val observeResponse: Future[ObserveResponse] = server.sendFut(client, request)
      observeResponse.filter(_.isSuccess).toUnit
    }
  }

  def cancelObserve(endpointName: String, path: LmPath): Future[Unit] = {
    withClient(endpointName) { client: Client =>
      Logger.debug(s"Observe cancel: ${path.toUrl}")
      server.getObservationRegistry.cancelObservation(client, path.toUrl)
      Future.successful(())
    }
  }

  def listenForChanges(listener: Listener): Unit = {
    changesManager.listenForChanges(listener)
  }

  def cancelListener(listener: Listener): Unit = {
    changesManager.cancelListener(listener)
  }

  private def withClient[R](endpointName: String)(fun: Client => Future[R]): Future[R] = {
    server.getClientRegistry.get(endpointName).opt.map(client => fun(client))
      .getOrElse(Future.failed(new IllegalArgumentException(s"Endpoint not registered: $endpointName")))
  }

  private def handleObjectInstance(objId: Int, discoveredUrlsMap: Map[String, Lwm2mDMNode], objInstace: LwM2mObjectInstance): Map[String, Lwm2mDMNode] = {
    val objInstanceUrl: String = s"/$objId/${objInstace.getId}"
    objInstace.getResources.asScala.values.foldLeft(discoveredUrlsMap) { (map, resource) =>
      val value = if (resource.isMultiInstances) {
        resource.getValues.values().asScala.toVector.toString
      } else {
        resource.getValue.toString
      }
      val url = s"$objInstanceUrl/${resource.getId}"
      map + (url -> map.get(url)
        .map(_.copy(value = value))
        .getOrElse(Lwm2mDMNode(url, value, Map.empty, objectsSpec.getDetails(url.lmPath), isObserved = false)))
    }
  }

  private def toLwm2mNode(path: LmPath, value: String): LwM2mNode = {
    val details: DMNodeDetails = objectsSpec.getDetails(path)
    val id = path.resourceId
    details.`type`.getOrElse("string") match {
      case "string" => LwM2mSingleResource.newStringResource(id, value)
      case "boolean" => LwM2mSingleResource.newBooleanResource(id, value.toBoolean)
      case "integer" => LwM2mSingleResource.newIntegerResource(id, value.toInt)
      case "float" => LwM2mSingleResource.newFloatResource(id, value.toInt)
      case "time" => LwM2mSingleResource.newDateResource(id, new Date(value.toLong))
      case "opaque" => LwM2mSingleResource.newBinaryResource(id, value.getBytes(Charset.forName("UTF-8")))
    }
  }

  def ledDemo(definition: LedDemoDefImpl): Future[Unit] = {
    val ordering = definition.ordering.split(',').map { endpointName =>
      val ep = endpointName.trim
      server.getClientRegistry.get(ep).opt.getOrElse {
        throw new IllegalArgumentException(s"Endpoint does not exists: $ep")
      }
      ep
    }
    val it = ordering.iterator
    @tailrec
    def run(previous: Future[Unit], iterator: Iterator[String]): Future[Unit] = {
      if (iterator.hasNext) {
        val client = iterator.next()
        val onFut = write(client, definition.path.lmPath, definition.onValue)
        run(onFut.flatMap { _ =>
          val next = Promise[Unit]
          scheduler.schedule(new Runnable {
            override def run(): Unit = {
              next.completeWith(write(client, definition.path.lmPath, definition.offValue))
            }
          }, definition.intervalMillis, TimeUnit.MILLISECONDS)
          next.future
        }, iterator)
      } else {
        previous
      }
    }
    Logger.info(s"Running demo: $definition")
    run(Future.successful(()), it)
  }

  def destroy(): Unit = {
    Logger.info(s"Lwm2m Service destroyed: $this")
    server.destroy()
  }

  override def toString = s"Lwm2mService($bindHostname, $bindPort)"
}
