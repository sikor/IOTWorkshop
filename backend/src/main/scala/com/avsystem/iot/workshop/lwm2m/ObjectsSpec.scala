package com.avsystem.iot.workshop
package lwm2m

import java.nio.channels.Channels

import com.avsystem.commons.serialization.GenCodec
import com.avsystem.iot.workshop.lwm2m.LmPathUtils.LmPath
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

    def toDetails: DMNodeDetails = {
      DMNodeDetails(name.opt, description.opt)
    }
  }

  case class ResourceSpec(name: String, id: Int, mandatory: Boolean, instancetype: String, description: String,
                          `type`: String, range: String, units: String, operations: String) {

    def toDetails: DMNodeDetails = DMNodeDetails(name.opt, description.opt, operations.opt, units.opt, `type`.opt)
  }

}

class ObjectsSpec(resources: Vector[String]) {

  private val dictionary = resources.flatMap { resourceName =>
    val is = getClass.getResourceAsStream(resourceName)
    val input = new JsonInput(Parser.parseFromChannel(Channels.newChannel(is))(JawnFacade).get)
    GenCodec.read[Vector[ObjectSpec]](input)
  }.map(objectSpec => (objectSpec.id, objectSpec)).toMap

  def suffixToHumanReadable(path: LmPath): String = {
    path.split match {
      case Array(a) => dictionary(a.toInt).name
      case Array(a, b) => b
      case Array(a, b, c) => dictionary(a.toInt).resourcesMap(c.toInt).name
    }
  }

  def getDetails(path: LmPath): DMNodeDetails = {
    path.split match {
      case Array(a) => dictionary(a.toInt).toDetails
      case Array(a, b) => DMNodeDetails.Empty
      case Array(a, b, c) => dictionary(a.toInt).resourcesMap(c.toInt).toDetails
    }
  }

}
