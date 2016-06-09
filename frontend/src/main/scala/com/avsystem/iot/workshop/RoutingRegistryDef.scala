package com.avsystem.iot.workshop

import io.udash._
import io.udash.utils.Bidirectional

object RoutingRegistryDef extends RoutingRegistry[RoutingState] {
  def matchUrl(url: Url): RoutingState =
    url2State.applyOrElse(url.value.stripSuffix("/"), (x: String) => ErrorState)

  def matchState(state: RoutingState): Url =
    Url(state2Url.apply(state))

  implicit class RoutingStateOps(private val state: RoutingState) extends AnyVal {
    def url: String = s"#${matchState(state).value}"
  }


  private val (url2State, state2Url) = Bidirectional[String, RoutingState] {
    case "" => IndexState
    case "/binding" => BindingDemoState("")
    case "/binding" /:/ arg => BindingDemoState(arg)
    case "/rpc" => RPCDemoState
    case "/scalacss" => DemoStylesState
  }

}