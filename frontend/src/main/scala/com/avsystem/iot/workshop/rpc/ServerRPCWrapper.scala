package com.avsystem.iot.workshop.rpc

import com.avsystem.iot.workshop.lwm2m.NodeUpdate
import org.scalajs
import org.scalajs.dom

class ServerRPCWrapper(private val serverRPC: MainServerRPC, private val clientRPCImpl: MainClientRPCImpl) {

  scalajs.dom.window.setInterval(() => serverRPC.pushMe(), 1000)

  def listenForChanges(endpoint: String)(listener: Vector[NodeUpdate] => Unit): Unit = {
    clientRPCImpl.listenForEndpoint(endpoint, listener)
    serverRPC.getLwm2mRPC.listenForChanges(endpoint)
  }

}
