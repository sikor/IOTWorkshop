package com.avsystem.iot.workshop.views

import io.udash.{DefaultViewPresenterFactory, bindings, _}
import com.avsystem.iot.workshop.RPCDemoState
import org.scalajs.dom.Element
import com.avsystem.iot.workshop.styles.DemoStyles

import scalacss.ScalatagsCss._
import scala.util.{Failure, Success}

case object RPCDemoViewPresenter extends DefaultViewPresenterFactory[RPCDemoState.type](() => {
  import com.avsystem.iot.workshop.Context._

  val serverResponse = Property[String]("???")
  val input = Property[String]("")
  input.listen((value: String) => {
    serverRpc.hello(value).onComplete {
      case Success(resp) => serverResponse.set(resp)
      case Failure(_) => serverResponse.set("Error")
    }
  })

  serverRpc.pushMe()

  new RPCDemoView(input, serverResponse)
})

class RPCDemoView(input: Property[String], serverResponse: Property[String]) extends View {
  import scalatags.JsDom.all._

  private val content = div(
    h2(
      "You can find this demo source code in: ",
      i("com.avsystem.iot.workshop.views.RPCDemoView")
    ),
    h3("Example"),
    bindings.TextInput(input, placeholder := "Type your name..."),
    p("Server response: ", bind(serverResponse)),
    h3("Read more"),
    a(DemoStyles.underlineLinkBlack)(href := "http://guide.udash.io/#/rpc", target := "_blank")("Read more in Udash Guide.")
  ).render

  override def getTemplate: Element = content

  override def renderChild(view: View): Unit = {}
}