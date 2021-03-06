package com.avsystem.iot.workshop.rpc

import com.avsystem.commons.rpc.RPC
import com.avsystem.iot.workshop.lwm2m.{ClientData, LedDemoDefImpl, RegisteredClient}

import scala.concurrent.Future

@RPC
trait Lwm2mRPC {
  def retrieveRegisteredClients(): Future[Vector[RegisteredClient]]

  def retrieveClientData(endpointName: String): Future[ClientData]

  def write(endpointName: String, path: String, value: String): Future[Unit]

  def observe(endpointName: String, path: String): Future[Unit]

  def cancelObserve(endpointName: String, path: String): Future[Unit]

  def listenForChanges(endpointName: String): Future[Unit]

  def cancelListener(endpointName: String): Future[Unit]

  def ledDemo(definition: LedDemoDefImpl): Future[Unit]
}
