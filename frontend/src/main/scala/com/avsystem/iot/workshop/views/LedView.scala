package com.avsystem.iot.workshop.views

import io.udash._
import com.avsystem.iot.workshop._
import com.avsystem.iot.workshop.lwm2m.{LedDemoDef, LedDemoDefImpl, RegisteredClient}
import org.scalajs.dom.Element
import com.avsystem.iot.workshop.styles.{DemoStyles, GlobalStyles}
import io.udash.core.DefaultViewPresenterFactory

import scala.util.{Failure, Success}
import scalacss.ScalatagsCss._


object LedViewPresenter extends DefaultViewPresenterFactory[DevicesState.type](() => new LedView) {


}

class LedView extends View {

  import com.avsystem.iot.workshop.Context._
  import scalatags.JsDom.all._
  import RoutingRegistryDef._

  val ledDefForm: ModelProperty[LedDemoDef] = ModelProperty[LedDemoDef]
  val status: Property[String] = Property[String]("Run by clicking button")


  private val content = div(
    h2("Led Demo"),
    div(p("Path"), TextInput(ledDefForm.subProp(_.path))),
    div(p("on value"), TextInput(ledDefForm.subProp(_.onValue))),
    div(p("off value"), TextInput(ledDefForm.subProp(_.offValue))),
    div(p("endpoints (comma separated)"), TextArea(ledDefForm.subProp(_.ordering), cols := 100, rows := 10)),
    div(p("interval millis"), NumberInput(ledDefForm.subProp(_.intervalMillis).transform(_.toString, _.toInt))),
    div(button(onclick :+= { ev: org.scalajs.dom.Event =>
      val values = ledDefForm.get
      status.set("Demo started!")
      serverRpc.getLwm2mRPC.ledDemo(LedDemoDefImpl(values.ordering, values.intervalMillis, values.path, values.onValue, values.offValue)).onComplete {
        case Success(_) => status.set("Demo Finished!")
        case Failure(ex) => status.set(s"Demo failed: $ex")
      }
      true
    })("Start")),
    p(bind(status))
  ).render

  override def getTemplate: Element = content

  override def renderChild(view: View): Unit = {}
}