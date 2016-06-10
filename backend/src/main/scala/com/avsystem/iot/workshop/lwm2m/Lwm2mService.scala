package com.avsystem.iot.workshop
package lwm2m

import org.eclipse.leshan.server.californium.LeshanServerBuilder
import org.eclipse.leshan.server.californium.impl.LeshanServer
import com.avsystem.commons.jiop.BasicJavaInterop._
import org.eclipse.leshan.server.client.Client
import Lwm2mService._
import org.eclipse.leshan.LinkObject
import org.slf4j.LoggerFactory

object Lwm2mService {

  private val Logger = LoggerFactory.getLogger(Lwm2mService.getClass)

  implicit class LeshanClientOps(private val client: Client) {

    def toRegisteredClient: RegisteredClient = {
      RegisteredClient(client.getRegistrationDate, client.getAddress.toString, client.getPort, client.getLifeTimeInSec,
        client.getEndpoint, client.getRegistrationId, client.getAdditionalRegistrationAttributes.asScala.toMap,
        client.getRootPath, client.getLastUpdate, client.getObjectLinks.map(_.getUrl).toVector)
    }
  }

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

  def destroy(): Unit = {
    Logger.info(s"Lwm2m Service destroyed: $this")
    server.destroy()
  }

  override def toString = s"Lwm2mService($bindHostname, $bindPort)"
}
