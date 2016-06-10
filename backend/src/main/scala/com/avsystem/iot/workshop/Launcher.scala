package com.avsystem.iot.workshop

import com.avsystem.iot.workshop.jetty.ApplicationServer
import com.avsystem.iot.workshop.lwm2m.Lwm2mService
import com.avsystem.iot.workshop.rpc.MainServerRPCImpl

object Launcher {
  def main(args: Array[String]): Unit = {
    val lwm2mService = new Lwm2mService("localhost", 5683)
    val server = new ApplicationServer(8080, "backend/target/UdashStatic/WebContent",
      implicit clientId => new MainServerRPCImpl(lwm2mService))
    server.start()
  }
}

       