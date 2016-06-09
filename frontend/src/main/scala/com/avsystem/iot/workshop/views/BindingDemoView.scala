package com.avsystem.iot.workshop.views

import io.udash._
import com.avsystem.iot.workshop.BindingDemoState
import org.scalajs.dom.Element
import com.avsystem.iot.workshop.styles.DemoStyles
import scalacss.ScalatagsCss._

case class BindingDemoViewPresenter(urlArg: String) extends DefaultViewPresenterFactory[BindingDemoState](() => {
  import com.avsystem.iot.workshop.Context._

  val model = Property[String](urlArg)
  new BindingDemoView(model)
})

class BindingDemoView(model: Property[String]) extends View {
  import scalatags.JsDom.all._

  private val content = div(
    h2(
      "You can find this demo source code in: ",
      i("com.avsystem.iot.workshop.views.BindingDemoView")
    ),
    h3("Example"),
    TextInput(model, placeholder := "Type something..."),
    p("You typed: ", bind(model)),
    h3("Read more"),
    a(DemoStyles.underlineLinkBlack)(href := "http://guide.udash.io/#/frontend/bindings", target := "_blank")("Read more in Udash Guide.")
  ).render

  override def getTemplate: Element = content

  override def renderChild(view: View): Unit = {}
}