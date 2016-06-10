package com.avsystem.iot.workshop.rpc

class MainClientRPCImpl extends MainClientRPC {
  override def push(number: Int): Unit =
    println(s"Push from server: $number")
}

       