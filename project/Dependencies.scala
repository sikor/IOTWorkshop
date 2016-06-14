import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._

object Dependencies extends Build {
  val udashVersion = "0.2.0"
  val udashJQueryVersion = "1.0.0"
  val logbackVersion = "1.1.4"
  val jettyVersion = "9.3.8.v20160314"
  val avsCommonsVersion = "1.10.6"
  val guavaVersion = "18.0"
  val slf4jVersion = "1.7.16"
  val scalatestVersion = "2.2.5"

  val leshanDeps = Seq(
    "org.eclipse.leshan" % "leshan-core" % "0.1.11-M10",
    "org.eclipse.leshan" % "leshan-server-cf" % "0.1.11-M10",
    "org.eclipse.leshan" % "leshan-server-core" % "0.1.11-M10"
  )

  val otherDeps = Seq(
    "com.google.guava" % "guava" % guavaVersion,
    "com.google.code.findbugs" % "jsr305" % "3.0.+"
  )

  val crossDeps = Def.setting(Seq[ModuleID](
    "io.udash" %%% "udash-core-shared" % udashVersion,
    "io.udash" %%% "udash-rpc-shared" % udashVersion
  ))

  val frontendDeps = Def.setting(Seq[ModuleID](
    "io.udash" %%% "udash-core-frontend" % udashVersion,
    "io.udash" %%% "udash-jquery" % udashJQueryVersion,
    "io.udash" %%% "udash-rpc-frontend" % udashVersion,
    "com.github.japgolly.scalacss" %%% "core" % "0.4.1",
    "com.github.japgolly.scalacss" %%% "ext-scalatags" % "0.4.1"
  ) ++ otherDeps)

  val frontendJSDeps = Def.setting(Seq[org.scalajs.sbtplugin.JSModuleID](
  ))

  val backendDeps = Def.setting(Seq[ModuleID](
    "ch.qos.logback" % "logback-classic" % logbackVersion,
    "org.eclipse.jetty" % "jetty-server" % jettyVersion,
    "org.eclipse.jetty" % "jetty-servlet" % jettyVersion,
    "io.udash" %% "udash-rpc-backend" % udashVersion,
    "org.eclipse.jetty.websocket" % "websocket-server" % jettyVersion,
    "org.scalatest" %% "scalatest" % scalatestVersion % Test
  ) ++ leshanDeps ++ otherDeps)

  val standardDependencyOverrides = Seq(
    "org.slf4j" % "slf4j-api" % slf4jVersion,
    "ch.qos.logback" % "logback-classic" % logbackVersion
  )

}