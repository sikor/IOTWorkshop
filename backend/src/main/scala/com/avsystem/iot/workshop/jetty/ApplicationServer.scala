package com.avsystem.iot.workshop.jetty

import com.avsystem.iot.workshop.rpc.MainServerRPC
import io.udash.rpc.ClientId
import io.udash.rpc.utils.DefaultAtmosphereFramework
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.gzip.GzipHandler
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.{DefaultServlet, ServletContextHandler, ServletHolder}

import scala.concurrent.ExecutionContext

class ApplicationServer(val port: Int, resourceBase: String, rpcProvider: ClientId => MainServerRPC) {
  private val server = new Server(port)
  private val contextHandler = new ServletContextHandler

  contextHandler.setSessionHandler(new SessionHandler)
  contextHandler.setGzipHandler(new GzipHandler)
  server.setHandler(contextHandler)

  def start() = server.start()

  def stop() = server.stop()

  private val appHolder = {
    val appHolder = new ServletHolder(new DefaultServlet)
    appHolder.setAsyncSupported(true)
    appHolder.setInitParameter("resourceBase", resourceBase)
    appHolder
  }
  contextHandler.addServlet(appHolder, "/*")

  private val atmosphereHolder = {
    import com.avsystem.iot.workshop.rpc._
    import io.udash.rpc._

    val config = new DefaultAtmosphereServiceConfig((clientId) => new DefaultExposesServerRPC(rpcProvider(clientId)))
    val framework = new DefaultAtmosphereFramework(config)(ExecutionContext.Implicits.global)

    //Disabling all files scan during service auto-configuration,
    //as it's quite time-consuming - a few seconds long.
    //
    //If it's really required, enable it, but at the cost of start-up overhead or some tuning has to be made.
    //For that purpose, check what is going on in:
    //- DefaultAnnotationProcessor
    //- org.atmosphere.cpr.AtmosphereFramework.autoConfigureService
    framework.allowAllClassesScan(false)

    framework.init()

    val atmosphereHolder = new ServletHolder(new RpcServlet(framework))
    atmosphereHolder.setAsyncSupported(true)
    atmosphereHolder
  }
  contextHandler.addServlet(atmosphereHolder, "/atm/*")

}

       