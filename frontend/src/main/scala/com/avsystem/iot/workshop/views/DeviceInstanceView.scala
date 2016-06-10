package com.avsystem.iot.workshop.views

import com.avsystem.iot.workshop.lwm2m.Lwm2mNode
import com.avsystem.iot.workshop.styles.GlobalStyles
import com.avsystem.iot.workshop.{DeviceInstanceState, DevicesState, RoutingRegistryDef}
import io.udash._
import io.udash.core.DefaultViewPresenterFactory
import org.scalajs.dom.Element

import scala.util.{Failure, Success}

case class DeviceInstanceViewPresenter(endpointName: String)
  extends DefaultViewPresenterFactory[DeviceInstanceState](() => new DeviceInstanceView(endpointName))

class DeviceInstanceView(endpointName: String) extends View {

  import com.avsystem.iot.workshop.Context._
  import scalatags.JsDom.all._
  import scalacss.ScalatagsCss._
  import RoutingRegistryDef._

  val clientData = serverRpc.getLwm2mRPC.retrieveClientData(endpointName)
  val statusProp = Property[String]("Retrieving data")
  val datamodelProp = SeqProperty[Lwm2mNode](Seq())


  clientData.onComplete {
    case Success(cd) =>
      statusProp.set("Got data from device")
      datamodelProp.set(cd.datamodel)
    case Failure(ex) => statusProp.set(s"Failed to retrieve data: $ex")
  }

  def tableLevel(path: String): Int = {
    path.count(_ == '/') - 1
  }

  def nested(path: String): Modifier = {
    path.count(_ == '/') match {
      case 1 => GlobalStyles.treeTableLvl0
      case 2 => GlobalStyles.treeTableLvl1
      case 3 => GlobalStyles.treeTableLvl2
    }
  }

  private val content = div(
    h2(s"Device: $endpointName"),
    p(bind(statusProp)),
    produce(datamodelProp)(datamodel =>
      table(GlobalStyles.table)(
        thead(td("Path"), td("Value"), td("Attributes")),
        tbody(datamodel.map(node =>
          tr(
            td(nested(node.path))(node.path),
            td(node.value.getOrElse("").toString),
            td(if (node.attributes.isEmpty) {
              ""
            } else {
              node.attributes.toString
            })
          )
        ))).render
    )
  ).render

  override def getTemplate: Element = content

  override def renderChild(view: View): Unit = {}
}
