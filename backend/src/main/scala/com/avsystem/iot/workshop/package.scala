package com.avsystem.iot

import com.avsystem.commons.SharedExtensions
import com.avsystem.iot.workshop.lwm2m.LmPathUtils

package object workshop extends SharedExtensions {
  implicit def stringOps(s: String): LmPathUtils.Ops = new LmPathUtils.Ops(s)
}