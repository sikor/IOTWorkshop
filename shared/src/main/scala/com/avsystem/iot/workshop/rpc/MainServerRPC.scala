package com.avsystem.iot.workshop.rpc

import com.avsystem.commons.rpc.RPC
import io.udash.rpc._
import scala.concurrent.Future

@RPC
trait MainServerRPC {
  def hello(name: String): Future[String]

  def pushMe(): Unit

  def getLwm2mRPC: Lwm2mRPC
}

       