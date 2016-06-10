package com.avsystem.iot.workshop.views

import io.udash._
import com.avsystem.iot.workshop._
import com.avsystem.iot.workshop.lwm2m.RegisteredClient
import org.scalajs.dom.Element
import com.avsystem.iot.workshop.styles.{DemoStyles, GlobalStyles}
import io.udash.core.DefaultViewPresenterFactory

import scala.util.{Failure, Success}
import scalacss.ScalatagsCss._


object DevicesViewPresenter extends DefaultViewPresenterFactory[DevicesState.type](() => new DevicesView)

class DevicesView extends View {

  import com.avsystem.iot.workshop.Context._
  import scalatags.JsDom.all._
  import RoutingRegistryDef._

  val registeredClientsFut = serverRpc.getLwm2mRPC.retrieveRegisteredClients()
  val registeredClientsProp = SeqProperty[RegisteredClient](Seq())
  val statusProp = Property[String]("Retrieving data")
  registeredClientsFut.onComplete {
    case Success(registeredClients) =>
      registeredClientsProp.set(registeredClients)
      statusProp.set(s"Number of connected devices: ${registeredClients.size}")
    case Failure(ex) => statusProp.set(s"Failed to retrieve devices list: $ex")
  }

  private val content = div(
    h2("Devices"),
    p(bind(statusProp)),
    produce(registeredClientsProp) { clients =>
      table(GlobalStyles.table)(
        thead(tr(td("Endpoint name"), td("Address"), td("Port"))),
        tbody(clients.map { client =>
          tr(td(client.endpointName), td(client.address.toString), td(client.port))
        })).render
    }
  ).render

  override def getTemplate: Element = content

  override def renderChild(view: View): Unit = {}
}