package com.avsystem.iot.workshop.rpc

import com.avsystem.commons.rpc.RPC
import com.avsystem.iot.workshop.lwm2m.{ClientData, RegisteredClient}

import scala.concurrent.Future

@RPC
trait Lwm2mRPC {
  def retrieveRegisteredClients(): Future[Vector[RegisteredClient]]

  def retrieveClientData(endpointName: String): Future[ClientData]
}
