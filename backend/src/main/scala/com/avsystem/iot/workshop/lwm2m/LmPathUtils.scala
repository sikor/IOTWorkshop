package com.avsystem.iot.workshop.lwm2m

object LmPathUtils {

  class LmPath(private val path: String) extends AnyVal {

    def split: Array[String] = {
      path.stripPrefix("/").stripSuffix("/").split('/')
    }

    def resourceId: Int = {
      split match {
        case Array(a, b, c) => c.toInt
        case _ => throw new IllegalArgumentException(s"Path does not contain resource Id: $path")
      }
    }

    def toUrl: String = path


    override def toString: String = s"LmPath($path)"
  }

  implicit class Ops(private val st: String) extends AnyVal {
    def lmPath: LmPath = new LmPath(st)
  }

}
