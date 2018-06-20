package co.ledger.manager.hsm.proxy

import co.ledger.manager.hsm.proxy.Application.server
import co.ledger.manager.hsm.proxy.scripts.EchoScript
import co.ledger.manager.hsm.proxy.server.{ScriptRunnerServer, WebSocketScriptRunnerServer}
import com.typesafe.config.{Config, ConfigFactory}

/**
  * Application entry point
  *
  * User: Pierre Pollastri
  * Date: 07-06-2018
  * Time: 14:02
  *
  */

trait ManagerHsmProxy {
  val scripts: Map[String, Script] = Map(
    "/echo" -> new EchoScript
  )
  val config: Config
  def server: ScriptRunnerServer = {
    config.getString("hsm.engine") match {
      case "websocket" =>
        new WebSocketScriptRunnerServer(config.getString("server.hostname"), config.getInt("server.port"), scripts)
      case unknown =>
        throw new Exception(s"Unknown server engine '$unknown'")
    }
  }

  val script: List[Script] = List(

  )

  def run(): Unit = server.run()

  def stop(): Unit = server.stop()
}

object Application extends App with ManagerHsmProxy {
  override val config: Config = ConfigFactory.load("application.conf")

   run()
}
