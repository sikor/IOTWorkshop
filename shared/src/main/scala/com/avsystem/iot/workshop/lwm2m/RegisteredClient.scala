package com.avsystem.iot.workshop.lwm2m

import java.net.InetAddress
import java.util.Date

import com.avsystem.commons.serialization.{GenCodec, Input, Output}

object RegisteredClient {
  implicit val Codec: GenCodec[RegisteredClient] = GenCodec.materialize[RegisteredClient]
}

case class RegisteredClient(registrationDate: Date, address: String, port: Int, lifeTimeInSec: Long,
                            endpointName: String, registrationId: String, registrationAttributes: Map[String, String],
                            rootPath: String, lastUpdate: Date, objectLinks: Vector[String])