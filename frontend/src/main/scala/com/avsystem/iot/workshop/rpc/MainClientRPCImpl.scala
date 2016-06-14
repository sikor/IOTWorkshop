package com.avsystem.iot.workshop.rpc

import com.avsystem.iot.workshop.lwm2m.NodeUpdate

import scala.concurrent.Future

class MainClientRPCImpl extends MainClientRPC {

  override def push(number: Int): Unit =
    println(s"Push from server: $number")

  private val listeners = new scala.collection.mutable.HashMap[String, (Vector[NodeUpdate] => Unit)]

  override def observationUpdate(endpoint: String, update: Vector[NodeUpdate]): Unit = {
    listeners.get(endpoint).foreach(_.apply(update))
  }

  def listenForEndpoint(endpoint: String, listener: (Vector[NodeUpdate] => Unit)): Unit = {
    listeners.put(endpoint, listener)
  }
}

       