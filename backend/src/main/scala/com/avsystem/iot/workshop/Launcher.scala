package com.avsystem.iot.workshop

import java.util.concurrent.TimeUnit

import com.avsystem.iot.workshop.jetty.ApplicationServer
import com.avsystem.iot.workshop.lwm2m.Lwm2mService
import com.avsystem.iot.workshop.rpc.MainServerRPCImpl
import com.google.common.cache._
import io.udash.rpc.ClientId
import org.slf4j.LoggerFactory

object Launcher {

  class ActiveClientsRegistry {
    private val Logger = LoggerFactory.getLogger(getClass)
    private val cache: Cache[String, String] = CacheBuilder
      .newBuilder()
      .expireAfterWrite(10, TimeUnit.SECONDS)
      .removalListener(new RemovalListener[String, String] {
        override def onRemoval(notification: RemovalNotification[String, String]): Unit = {
          if (notification.getCause == RemovalCause.EXPIRED) {
            Logger.warn(s"Client time out: ${notification.getKey}")
          }
        }
      })
      .build()

    def onPing(client: ClientId): Unit = {
      if (!cache.asMap().containsKey(client.id)) {
        Logger.info(s"new client: ${client.id}")
      }
      cache.put(client.id, client.id)
    }

    def isActive(client: ClientId): Boolean = {
      cache.asMap().containsKey(client.id)
    }
  }

  def main(args: Array[String]): Unit = {
    val clientsRegistry = new ActiveClientsRegistry
    var address: String = "localhost"
    var port: Int = 5683
    if (args.length == 2) {
      address = args(0)
      port = args(1).toInt
    }
    val lwm2mService = new Lwm2mService(address, port)
    val server = new ApplicationServer(8080, "backend/target/UdashStatic/WebContent",
      implicit clientId => new MainServerRPCImpl(lwm2mService, clientsRegistry))
    server.start()
  }
}

       