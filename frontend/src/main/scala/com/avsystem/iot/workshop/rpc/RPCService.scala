package com.avsystem.iot.workshop.rpc

class RPCService extends MainClientRPC {
  override def push(number: Int): Unit =
    println(s"Push from server: $number")
}

       