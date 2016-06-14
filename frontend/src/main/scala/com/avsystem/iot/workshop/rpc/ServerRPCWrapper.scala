package com.avsystem.iot.workshop.rpc

import com.avsystem.iot.workshop.lwm2m.NodeUpdate

class ServerRPCWrapper(private val serverRPC: MainServerRPC, private val clientRPCImpl: MainClientRPCImpl) {

  def listenForChanges(endpoint: String)(listener: Vector[NodeUpdate] => Unit): Unit = {
    clientRPCImpl.listenForEndpoint(endpoint, listener)
    serverRPC.getLwm2mRPC.listenForChanges(endpoint)
  }

}
