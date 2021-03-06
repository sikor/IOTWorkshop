package com.avsystem.iot.workshop.lwm2m

import java.util.Date

import com.avsystem.commons.misc.Opt
import com.avsystem.commons.serialization.GenCodec

object RegisteredClient {
  implicit val Codec: GenCodec[RegisteredClient] = GenCodec.materialize[RegisteredClient]
}

case class RegisteredClient(registrationDate: Date, address: String, port: Int, lifeTimeInSec: Long,
                            endpointName: String, registrationId: String, registrationAttributes: Map[String, String],
                            rootPath: String, lastUpdate: Date, objectLinks: Vector[String])

object DMNodeDetails {
  val Empty = DMNodeDetails()
  implicit val Codec: GenCodec[DMNodeDetails] = GenCodec.materialize[DMNodeDetails]
}

case class DMNodeDetails(name: Opt[String] = Opt.Empty, description: Opt[String] = Opt.Empty, operations: Opt[String] = Opt.Empty,
                         units: Opt[String] = Opt.Empty, `type`: Opt[String] = Opt.Empty)

object Lwm2mDMNode {
  implicit val Codec: GenCodec[Lwm2mDMNode] = GenCodec.materialize[Lwm2mDMNode]
}

trait Lwm2mDMNodeTemplate {
  def path: String

  def value: String

  def attributesSeq: Seq[(String, String)]

  def details: DMNodeDetails

  def isObserved: Boolean
}

case class Lwm2mDMNode(path: String, value: String, attributes: Map[String, String], details: DMNodeDetails,
                       isObserved: Boolean) extends Lwm2mDMNodeTemplate {
  override def attributesSeq: Seq[(String, String)] = attributes.toSeq
}

object ClientData {
  implicit val Codec: GenCodec[ClientData] = GenCodec.materialize[ClientData]
}

case class ClientData(registration: RegisteredClient, datamodel: Vector[Lwm2mDMNode])

case class NodeUpdate(path: String, isObserved: Opt[Boolean], value: Opt[String])

trait LedDemoDef {
  def ordering: String

  def intervalMillis: Int

  def path: String

  def onValue: String

  def offValue: String
}

object LedDemoDefImpl {
  implicit val Codec: GenCodec[LedDemoDefImpl] = GenCodec.materialize[LedDemoDefImpl]

}

case class LedDemoDefImpl(ordering: String, intervalMillis: Int, path: String, onValue: String, offValue: String)