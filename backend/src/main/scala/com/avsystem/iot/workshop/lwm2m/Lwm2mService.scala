package com.avsystem.iot.workshop
package lwm2m

import com.avsystem.commons.concurrent.RunInQueueEC
import com.avsystem.commons.jiop.BasicJavaInterop._
import com.avsystem.commons.misc.Opt
import com.avsystem.iot.workshop.lwm2m.Lwm2mService._
import org.eclipse.leshan.core.node.{LwM2mObject, LwM2mObjectInstance}
import org.eclipse.leshan.core.request.{DiscoverRequest, ReadRequest}
import org.eclipse.leshan.core.response.{DiscoverResponse, ErrorCallback, ReadResponse, ResponseCallback}
import org.eclipse.leshan.server.californium.LeshanServerBuilder
import org.eclipse.leshan.server.californium.impl.LeshanServer
import org.eclipse.leshan.server.client.Client
import org.slf4j.LoggerFactory

import scala.concurrent.{Future, Promise}

object Lwm2mService {

  private val Logger = LoggerFactory.getLogger(Lwm2mService.getClass)

  implicit class LeshanClientOps(private val client: Client) {

    def toRegisteredClient: RegisteredClient = {
      RegisteredClient(client.getRegistrationDate, client.getAddress.toString, client.getPort, client.getLifeTimeInSec,
        client.getEndpoint, client.getRegistrationId, client.getAdditionalRegistrationAttributes.asScala.toMap,
        client.getRootPath, client.getLastUpdate, client.getObjectLinks.map(_.getUrl).toVector)
    }
  }

  implicit val ec = RunInQueueEC
}

class Lwm2mService(val bindHostname: String, val bindPort: Int) {

  val builder: LeshanServerBuilder = new LeshanServerBuilder
  builder.setLocalAddress(bindHostname, bindPort)
  val server: LeshanServer = builder.build()
  server.start()

  Logger.info(s"Lwm2m Service started: $this")

  def retrieveAllRegisteredClients(): Vector[RegisteredClient] = {
    Logger.debug("retrieving all registered clients")
    server.getClientRegistry.allClients().asScala.map(_.toRegisteredClient).toVector
  }

  def retrieveClientData(endpointName: String): Future[ClientData] = {
    server.getClientRegistry.get(endpointName).opt.map { client =>
      val dataForObjects = client.getObjectLinks.map { linkObject =>
        val url = linkObject.getUrl
        if (url.length > 1) {
          val result = Promise[Vector[Lwm2mNode]]()
          val discoverRequest = new DiscoverRequest(url)
          val pattern = "\\d+".r
          val objId = pattern.findFirstIn(url).get.toInt
          server.send(client, discoverRequest, discoverResponseCallback(objId, client, result), errorCallback(result))
          result.future
        } else {
          Future.successful(Vector.empty)
        }
      }
      val datamodel = dataForObjects.foldLeft(Future.successful(Vector.empty[Lwm2mNode])) { (allNodesFut, nodesPart) =>
        allNodesFut.flatMap { allNodes =>
          nodesPart.map { part =>
            allNodes ++ part
          }
        }
      }
      datamodel.map(dm => ClientData(client.toRegisteredClient, dm.sortBy(_.path)))
    }.getOrElse(Future.failed(new IllegalArgumentException(s"Endpoint not registered: $endpointName")))
  }

  private def discoverResponseCallback(objId: Int, client: Client, result: Promise[Vector[Lwm2mNode]]) =
    new ResponseCallback[DiscoverResponse] {
      override def onResponse(response: DiscoverResponse): Unit = {
        val objUrl: String = s"/$objId"
        val discoveredUrlsMap = response.getObjectLinks.map(ol => (ol.getUrl, Lwm2mNode(ol.getUrl, Opt.Empty,
          ol.getAttributes.asScala.mapValues(_.toString).toMap))).toMap + (objUrl -> Lwm2mNode(objUrl, Opt.Empty, Map.empty))
        val readValueRequest = new ReadRequest(objUrl)
        server.send(client, readValueRequest, readValueResponseCallback(objId, discoveredUrlsMap, result), errorCallback(result))
      }
    }

  private def handleObjectInstance(objId: Int, discoveredUrlsMap: Map[String, Lwm2mNode], objInstace: LwM2mObjectInstance): Map[String, Lwm2mNode] = {
    objInstace.getResources.asScala.values.foldLeft(discoveredUrlsMap) { (map, resource) =>
      val value = if (resource.isMultiInstances) {
        resource.getValues.values().asScala.toString
      } else {
        resource.getValue.toString
      }
      val url = s"/$objId/${objInstace.getId}/${resource.getId}"
      map + (url -> map.get(url).map(_.copy(value = value.opt)).getOrElse(Lwm2mNode(url, value.opt, Map.empty)))
    }
  }

  private def readValueResponseCallback(objId: Int, discoveredUrlsMap: Map[String, Lwm2mNode], result: Promise[Vector[Lwm2mNode]]) =
    new ResponseCallback[ReadResponse] {
      override def onResponse(response: ReadResponse): Unit = {
        response.getContent match {
          case obj: LwM2mObject =>
            val updatedMap = obj.getInstances.asScala.values.foldLeft(discoveredUrlsMap) { (map, objInstace) =>
              handleObjectInstance(obj.getId, map, objInstace)
            }
            result.success(updatedMap.values.toVector)
          case objInstance: LwM2mObjectInstance =>
            val updatedMap = handleObjectInstance(objId, discoveredUrlsMap, objInstance)
            result.success(updatedMap.values.toVector)
          case _ => result.failure(new IllegalStateException(s"Unexpected content: ${response.getContent}"))
        }
      }
    }

  def errorCallback[T](result: Promise[T]): ErrorCallback = new ErrorCallback {
    override def onError(e: Exception): Unit = result.failure(e)
  }

  def destroy(): Unit = {
    Logger.info(s"Lwm2m Service destroyed: $this")
    server.destroy()
  }

  override def toString = s"Lwm2mService($bindHostname, $bindPort)"
}
