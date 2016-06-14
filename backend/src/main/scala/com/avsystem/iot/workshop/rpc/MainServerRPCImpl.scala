package com.avsystem.iot.workshop.rpc

import com.avsystem.iot.workshop.Launcher.ActiveClientsRegistry
import com.avsystem.iot.workshop.lwm2m.Lwm2mService
import io.udash.rpc._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class MainServerRPCImpl(lwm2mService: Lwm2mService, clientsRegistry: ActiveClientsRegistry)(implicit clientId: ClientId) extends MainServerRPC {
  override def hello(name: String): Future[String] =
    Future.successful(s"Hello, $name!")

  override def pushMe(): Unit = {
    clientsRegistry.onPing(clientId)
  }

  override val getLwm2mRPC: Lwm2mRPCImpl = new Lwm2mRPCImpl(lwm2mService, clientId, clientsRegistry)
}

       