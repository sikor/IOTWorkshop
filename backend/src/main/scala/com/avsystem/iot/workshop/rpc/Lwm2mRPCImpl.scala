package com.avsystem.iot.workshop.rpc

import com.avsystem.iot.workshop.lwm2m.{ClientData, Lwm2mService, RegisteredClient}

import scala.concurrent.Future

class Lwm2mRPCImpl(private val lwm2mService: Lwm2mService) extends Lwm2mRPC {
  override def retrieveRegisteredClients(): Future[Vector[RegisteredClient]] =
    Future.successful(lwm2mService.retrieveAllRegisteredClients())

  override def retrieveClientData(endpointName: String): Future[ClientData] = {
    lwm2mService.retrieveClientData(endpointName)
  }
}
