package com.avsystem.iot.workshop
package rpc

import com.avsystem.commons.concurrent.RunInQueueEC
import com.avsystem.iot.workshop.lwm2m.ChangesManager.Listener
import com.avsystem.iot.workshop.lwm2m.{ClientData, Lwm2mService, NodeUpdate, RegisteredClient}
import com.avsystem.iot.workshop.rpc.Lwm2mRPCImpl.ListenerToken
import io.udash.rpc.ClientId

import scala.concurrent.{ExecutionContext, Future}

object Lwm2mRPCImpl {

  case class ListenerToken(endpointName: String, clientId: ClientId) extends Listener {
    override def onUpdate(update: Vector[NodeUpdate]): Unit =
      ClientRPC(clientId)(ExecutionContext.Implicits.global).observationUpdate(endpointName, update)
  }

}

class Lwm2mRPCImpl(private val lwm2mService: Lwm2mService, private val clientId: ClientId) extends Lwm2mRPC {

  override def retrieveRegisteredClients(): Future[Vector[RegisteredClient]] =
    Future.successful(lwm2mService.retrieveAllRegisteredClients())

  override def retrieveClientData(endpointName: String): Future[ClientData] = {
    lwm2mService.retrieveClientData(endpointName)
  }

  override def write(endpointName: String, path: String, value: String): Future[Unit] = {
    lwm2mService.write(endpointName, path.lmPath, value)
  }

  override def observe(endpointName: String, path: String): Future[Unit] = {
    lwm2mService.observe(endpointName, path.lmPath)
  }

  override def cancelObserve(endpointName: String, path: String): Future[Unit] = {
    lwm2mService.cancelObserve(endpointName, path.lmPath)
  }

  override def listenForChanges(endpointName: String): Future[Unit] = {
    lwm2mService.listenForChanges(ListenerToken(endpointName, clientId))
    Future.successful(())
  }

  override def cancelListener(endpointName: String): Future[Unit] = {
    lwm2mService.cancelListener(ListenerToken(endpointName, clientId))
    Future.successful(())
  }
}
