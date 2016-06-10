package com.avsystem.iot.workshop

import com.avsystem.iot.workshop.views.{DevicesViewPresenter, ErrorViewPresenter, _}
import io.udash.{RoutingRegistry, _}
import io.udash.routing./:/
import io.udash.utils.Bidirectional

sealed abstract class RoutingState(val parentState: RoutingState) extends State

case object RootState extends RoutingState(null)

case object DevicesState extends RoutingState(RootState)

case object ErrorState extends RoutingState(RootState)

case object IndexState extends RoutingState(RootState)

case class BindingDemoState(urlArg: String = "") extends RoutingState(RootState)

case object RPCDemoState extends RoutingState(RootState)

case object DemoStylesState extends RoutingState(RootState)


object RoutingRegistryDef extends RoutingRegistry[RoutingState] {
  def matchUrl(url: Url): RoutingState =
    url2State.applyOrElse(url.value.stripSuffix("/"), (x: String) => ErrorState)

  def matchState(state: RoutingState): Url =
    Url(state2Url.apply(state))

  implicit class RoutingStateOps(private val state: RoutingState) extends AnyVal {
    def url: String = s"#${matchState(state).value}"
  }


  private val (url2State, state2Url) = Bidirectional[String, RoutingState] {
    case "" => DevicesState
    case "/binding" => BindingDemoState("")
    case "/binding" /:/ arg => BindingDemoState(arg)
    case "/rpc" => RPCDemoState
    case "/scalacss" => DemoStylesState
  }

}


object StatesToViewPresenterDef extends ViewPresenterRegistry[RoutingState] {
  def matchStateToResolver(state: RoutingState): ViewPresenter[_ <: RoutingState] = state match {
    case RootState => RootViewPresenter
    case IndexState => IndexViewPresenter
    case BindingDemoState(urlArg) => BindingDemoViewPresenter(urlArg)
    case RPCDemoState => RPCDemoViewPresenter
    case DemoStylesState => DemoStylesViewPresenter
    case DevicesState => DevicesViewPresenter
    case _ => ErrorViewPresenter
  }
}