package com.avsystem.iot.workshop.lwm2m

import com.avsystem.commons.misc.Opt


case class Lwm2mDmNode(objectId: Int, objectInstanceId: Opt[Int], resourceId: Opt[Int], attributes: Map[String, String]) {
  require(resourceId.isEmpty || objectInstanceId.isDefined)
}