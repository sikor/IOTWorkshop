package com.avsystem.iot.workshop.rpc

import com.avsystem.commons.rpc.RPC
import com.avsystem.iot.workshop.lwm2m.NodeUpdate
import io.udash.rpc._

import scala.concurrent.Future

@RPC
trait MainClientRPC {
  def push(number: Int): Unit

  def observationUpdate(endpoint: String, update: Vector[NodeUpdate]): Unit
}
       