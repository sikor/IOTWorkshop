package com.avsystem.iot.workshop.lwm2m

class ObjectsSpecTest extends org.scalatest.FunSuite {

  test("Read spec") {
    val s = new ObjectsSpec(Vector("/oma-objects-spec.json", "/custom-objects-spec.json"))
    assert(s.suffixToHumanReadable("/0") == "LWM2M Security")
    assert(s.suffixToHumanReadable("/0/1/0") == "LWM2M  Server URI")
    assert(s.suffixToHumanReadable("/31337/0/2") == "Default Route")
  }
}
