package com.avsystem.iot.workshop

import io.udash._

sealed abstract class RoutingState(val parentState: RoutingState) extends State

case object RootState extends RoutingState(null)

case object DevicesState extends RoutingState(RootState)

case object ErrorState extends RoutingState(RootState)

case object IndexState extends RoutingState(RootState)

case class BindingDemoState(urlArg: String = "") extends RoutingState(RootState)

case object RPCDemoState extends RoutingState(RootState)

case object DemoStylesState extends RoutingState(RootState)