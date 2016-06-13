package com.avsystem.iot.workshop.lwm2m

import java.nio.channels.Channels

import com.avsystem.commons.serialization.GenCodec
import com.avsystem.iot.workshop.lwm2m.ObjectsSpec.ObjectSpec
import io.udash.rpc.serialization.JsonInput
import io.udash.rpc.serialization.jawn.JawnFacade
import jawn.Parser

object ObjectsSpec {

  implicit val ResourceSpecCodec = GenCodec.materialize[ResourceSpec]
  implicit val ObjectSpecCodec = GenCodec.materialize[ObjectSpec]

  case class ObjectSpec(name: String, id: Int, mandatory: Boolean, instancetype: String, description: String,
                        resourcedefs: Vector[ResourceSpec]) {
    val resourcesMap = resourcedefs.map(r => (r.id, r)).toMap
  }

  case class ResourceSpec(name: String, id: Int, mandatory: Boolean, instancetype: String, description: String,
                          `type`: String, range: String, units: String, operations: String)
}

class ObjectsSpec(resources: Vector[String]) {

  private val dictionary = resources.flatMap { resourceName =>
    val is = getClass.getResourceAsStream(resourceName)
    val input = new JsonInput(Parser.parseFromChannel(Channels.newChannel(is))(JawnFacade).get)
    GenCodec.read[Vector[ObjectSpec]](input)
  }.map(objectSpec => (objectSpec.id, objectSpec)).toMap

  def suffixToHumanReadable(path: String): String = {
    splitPath(path) match {
      case Array(a) => dictionary(a.toInt).name
      case Array(a, b) => b
      case Array(a, b, c) => dictionary(a.toInt).resourcesMap(c.toInt).name
    }
  }

  private def splitPath(path: String): Array[String] = {
    path.stripPrefix("/").stripSuffix("/").split('/')
  }
}
