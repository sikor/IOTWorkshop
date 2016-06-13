package com.avsystem.iot.workshop
package rpc

import com.avsystem.iot.workshop.lwm2m.{ClientData, Lwm2mService, RegisteredClient}

import scala.concurrent.Future

class Lwm2mRPCImpl(private val lwm2mService: Lwm2mService) extends Lwm2mRPC {
  override def retrieveRegisteredClients(): Future[Vector[RegisteredClient]] =
    Future.successful(lwm2mService.retrieveAllRegisteredClients())

  override def retrieveClientData(endpointName: String): Future[ClientData] = {
    lwm2mService.retrieveClientData(endpointName)
  }

  override def write(endpointName: String, path: String, value: String): Future[Unit] = {
    lwm2mService.write(endpointName, path.lmPath, value)
  }
}
