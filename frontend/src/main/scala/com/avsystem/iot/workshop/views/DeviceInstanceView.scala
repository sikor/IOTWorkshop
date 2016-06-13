package com.avsystem.iot.workshop.views

import com.avsystem.iot.workshop.lwm2m.Lwm2mDMNode
import com.avsystem.iot.workshop.styles.GlobalStyles
import com.avsystem.iot.workshop.{DeviceInstanceState, DevicesState, RoutingRegistryDef}
import io.udash._
import io.udash.core.DefaultViewPresenterFactory
import io.udash.properties.SeqProperty
import org.scalajs.dom.Element

import scala.util.{Failure, Success}
import scalatags.JsDom

case class DeviceInstanceViewPresenter(endpointName: String)
  extends DefaultViewPresenterFactory[DeviceInstanceState](() => new DeviceInstanceView(endpointName))

class DeviceInstanceView(endpointName: String) extends View {

  import com.avsystem.iot.workshop.Context._
  import scalatags.JsDom.all._
  import scalacss.ScalatagsCss._
  import RoutingRegistryDef._

  val clientData = serverRpc.getLwm2mRPC.retrieveClientData(endpointName)
  val statusProp = Property[String]("Retrieving data")
  val datamodelProp = SeqProperty[Lwm2mDMNode](Seq())


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
        thead(td("Path"), td("Value"), td("Type"), td("Operations"), td("Attributes")),
        tbody(datamodel.map(node =>
          tr(
            td(nested(node.path))(node.details.name.map(name => s"$name (${node.path})").getOrElse(node.path).toString),
            td(node.value.getOrElse("").toString + node.details.units.filter(_ != "").map(u => s" [$u]").getOrElse("")),
            td(node.details.`type`.getOrElse("").toString),
            td(node.details.operations.getOrElse("").toString),
            td(attributesTag(node))
          )
        ))).render
    )
  ).render

  private def attributesTag(node: Lwm2mDMNode): JsDom.StringFrag = {
    if (node.attributes.isEmpty) {
      ""
    } else {
      node.attributes.toString
    }
  }

  override def getTemplate: Element = content

  override def renderChild(view: View): Unit = {}
}
