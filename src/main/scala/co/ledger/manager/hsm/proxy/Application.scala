package co.ledger.manager.hsm.proxy

import co.ledger.manager.hsm.proxy.scripts.{EchoScript, UpdateFirmwareScript}
import co.ledger.manager.hsm.proxy.server.{ScriptRunnerServer, WebSocketScriptRunnerServer}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

/**
  * Application entry point
  *
  * User: Pierre Pollastri
  * Date: 07-06-2018
  * Time: 14:02
  *
  */

trait ManagerHsmProxy extends LazyLogging {
  lazy val scripts: Map[String, Script] = Map(
    "/echo" -> new EchoScript,
    "/install" -> new UpdateFirmwareScript(config)
  )
  val config: Config
  def server: ScriptRunnerServer = {
    config.getString("hsm.engine") match {
      case "websocket" =>
        val port =  config.getInt("server.port")
        val hostname = config.getString("server.hostname")
        logger.info(s"Server started on $hostname:$port")
        new WebSocketScriptRunnerServer(hostname, port, scripts)
      case unknown =>
        throw new Exception(s"Unknown server engine '$unknown'")
    }
  }

  def run(): Unit = server.run()

  def stop(): Unit = server.stop()
}

object Application extends App with ManagerHsmProxy {
  override val config: Config = ConfigFactory.defaultApplication()

   run()
}
