package com.avsystem.iot.workshop.views

import com.avsystem.commons.misc.Opt
import com.avsystem.iot.workshop.DeviceInstanceState
import com.avsystem.iot.workshop.lwm2m.Lwm2mDMNodeTemplate
import com.avsystem.iot.workshop.styles.GlobalStyles
import io.udash._
import io.udash.bindings.{Checkbox, TextInput}
import io.udash.core.DefaultViewPresenterFactory
import io.udash.properties.{ImmutableValue, ModelProperty, Property, SeqProperty}
import org.scalajs
import org.scalajs.dom.Element
import org.scalajs.dom.html.Input

import scala.util.{Failure, Success}
import scalatags.JsDom.TypedTag

case class DeviceInstanceViewPresenter(endpointName: String)
  extends DefaultViewPresenterFactory[DeviceInstanceState](() => new DeviceInstanceView(endpointName))

class DeviceInstanceView(endpointName: String) extends View {

  import com.avsystem.iot.workshop.Context._

  import scalacss.ScalatagsCss._
  import scalatags.JsDom.all._

  implicit def immutableOpt[I: ImmutableValue]: ImmutableValue[Opt[I]] = null

  val clientData = serverRpc.getLwm2mRPC.retrieveClientData(endpointName)
  val statusProp = Property[String]("Retrieving data")
  val datamodelProp = SeqProperty[Lwm2mDMNodeTemplate](Seq())

  serverRpcWrapper.listenForChanges(endpointName) { update =>
    update.foreach { nodeUpdate =>
      datamodelProp.elemProperties.foreach { row =>
        val rowModel: ModelProperty[Lwm2mDMNodeTemplate] = row.asModel
        if (rowModel.get.path == nodeUpdate.path) {
          if (nodeUpdate.value.isDefined) {
            if (rowModel.get.value != nodeUpdate.value.get) {
              rowModel.subProp(_.value).set(nodeUpdate.value.get)
            }
          }
          if (nodeUpdate.isObserved.isDefined) {
            if (rowModel.get.isObserved != nodeUpdate.isObserved.get) {
              rowModel.subProp(_.isObserved).set(nodeUpdate.isObserved.get)
            }
          }
        }
      }
    }
  }


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


  def valueForm(nodeModel: ModelProperty[Lwm2mDMNodeTemplate]) = td(
    {
      val unitString = nodeModel.get.details.units.filter(_ != "").map(u => s" [$u]").getOrElse("").toString
      val node = nodeModel.get
      if (node.details.operations.exists(_.exists(_ == 'W'))) {
        val valueProp = nodeModel.subProp(_.value)
        div(
          node.details.`type`.getOrElse("string") match {
            case _ => TextInput(valueProp)
          },
          unitString,
          button(onclick :+= { ev: scalajs.dom.Event =>
            serverRpc.getLwm2mRPC.write(endpointName, node.path, valueProp.get).onComplete {
              case Success(_) => println(s"value set: ${valueProp.get}")
              case Failure(ex) => println(s"Failed to set value: $ex")
            }
            true
          })("set")
        )
      } else {
        node.value.toString + unitString
      }
    }
  )

  private def observationForm(nodeModel: ModelProperty[Lwm2mDMNodeTemplate]) = td {
    val node = nodeModel.get
    if (node.details.operations.exists(_.exists(_ == 'R'))) {
      div(
        bind(nodeModel.subProp(_.isObserved)),
        button(onclick :+= { ev: org.scalajs.dom.Event =>
          serverRpc.getLwm2mRPC.observe(endpointName, node.path).onComplete {
            case Success(_) => println(s"Observe success: $endpointName, ${node.path}")
              nodeModel.subProp(_.isObserved).set(true)
            case Failure(ex) => println(s"Observe failure: $endpointName, ${node.path}, $ex")
          }
          true
        })("start"),
        button(onclick :+= { ev: org.scalajs.dom.Event =>
          serverRpc.getLwm2mRPC.cancelObserve(endpointName, node.path).onComplete {
            case Success(_) => println(s"Cancel Observe success: $endpointName, ${node.path}")
              nodeModel.subProp(_.isObserved).set(false)
            case Failure(ex) => println(s"Cancel Observe failure: $endpointName, ${node.path}, $ex")
          }
          true
        })("stop")
      )
    } else {
      " - "
    }
  }

  private val content = div(
    h2(s"Device: $endpointName"),
    p(bind(statusProp)),
    table(GlobalStyles.table)(
      thead(tr(td("Path"), td("Value"), td("Type"), td("Operations"), td("Is Observed"))),
      tbody(repeat(datamodelProp) { nodeProp =>
        val nodeModel: ModelProperty[Lwm2mDMNodeTemplate] = nodeProp.asModel
        val node = nodeModel.get
        tr(
          td(nested(node.path))(node.details.name.map(name => s"$name (${node.path})").getOrElse(node.path).toString),
          valueForm(nodeModel),
          td(node.details.`type`.getOrElse("").toString),
          td(node.details.operations.getOrElse("").toString),
          observationForm(nodeModel)
        ).render
      }))
  ).render


  override def getTemplate: Element = content

  override def renderChild(view: View): Unit = {}
}
