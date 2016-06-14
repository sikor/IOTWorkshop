package com.avsystem.iot.workshop

import io.udash.{StrictLogging, _}
import io.udash.wrappers.jquery._
import org.scalajs.dom.{Element, document}

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

object Context {
  implicit val executionContext = scalajs.concurrent.JSExecutionContext.Implicits.queue
  private val routingRegistry = RoutingRegistryDef
  private val viewPresenterRegistry = StatesToViewPresenterDef

  implicit val applicationInstance = new Application[RoutingState](routingRegistry, viewPresenterRegistry, RootState)

  import io.udash.rpc._
  import com.avsystem.iot.workshop.rpc._

  val clientRpc: MainClientRPCImpl = new MainClientRPCImpl
  val serverRpc = DefaultServerRPC[MainClientRPC, MainServerRPC](clientRpc)
  val serverRpcWrapper = new ServerRPCWrapper(serverRpc, clientRpc)

}

object Init extends JSApp with StrictLogging {

  @JSExport
  override def main(): Unit = {
    jQ(document).ready((_: Element) => {
      val appRoot = jQ("#application").get(0)
      if (appRoot.isEmpty) {
        logger.error("Application root element not found! Check your index.html file!")
      } else {
        Context.applicationInstance.run(appRoot.get)

        import scalacss.Defaults._
        import scalacss.ScalatagsCss._
        import scalatags.JsDom._
        import com.avsystem.iot.workshop.styles.GlobalStyles
        import com.avsystem.iot.workshop.styles.DemoStyles
        import com.avsystem.iot.workshop.styles.partials.FooterStyles
        import com.avsystem.iot.workshop.styles.partials.HeaderStyles
        jQ(GlobalStyles.render[TypedTag[org.scalajs.dom.raw.HTMLStyleElement]].render).insertBefore(appRoot.get)
        jQ(DemoStyles.render[TypedTag[org.scalajs.dom.raw.HTMLStyleElement]].render).insertBefore(appRoot.get)
        jQ(FooterStyles.render[TypedTag[org.scalajs.dom.raw.HTMLStyleElement]].render).insertBefore(appRoot.get)
        jQ(HeaderStyles.render[TypedTag[org.scalajs.dom.raw.HTMLStyleElement]].render).insertBefore(appRoot.get)
      }
    })
  }
}