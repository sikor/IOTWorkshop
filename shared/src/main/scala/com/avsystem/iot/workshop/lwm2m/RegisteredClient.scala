package com.avsystem.iot.workshop.lwm2m

import java.net.InetAddress
import java.util.Date

import com.avsystem.commons.misc.Opt
import com.avsystem.commons.serialization.{GenCodec, Input, Output}

object RegisteredClient {
  implicit val Codec: GenCodec[RegisteredClient] = GenCodec.materialize[RegisteredClient]
}

case class RegisteredClient(registrationDate: Date, address: String, port: Int, lifeTimeInSec: Long,
                            endpointName: String, registrationId: String, registrationAttributes: Map[String, String],
                            rootPath: String, lastUpdate: Date, objectLinks: Vector[String])

object Lwm2mDMNode {
  implicit val Codec: GenCodec[Lwm2mDMNode] = GenCodec.materialize[Lwm2mDMNode]
}

case class Lwm2mDMNode(path: String, value: Opt[String], attributes: Map[String, String])

object ClientData {
  implicit val Codec: GenCodec[ClientData] = GenCodec.materialize[ClientData]
}

case class ClientData(registration: RegisteredClient, datamodel: Vector[Lwm2mDMNode])