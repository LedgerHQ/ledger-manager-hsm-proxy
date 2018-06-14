package co.ledger.manager.hsm.proxy

import co.ledger.manager.hsm.proxy.proxy.WebSocketScriptRunnerServer
import co.ledger.manager.hsm.proxy.server.ScriptRunnerServer
import com.typesafe.config.{Config, ConfigFactory}

/**
  * Application entry point
  *
  * User: Pierre Pollastri
  * Date: 07-06-2018
  * Time: 14:02
  *
  */

trait ApplImpl extends App {
  val config: Config
  lazy val server: ScriptRunnerServer = {
    config.getString("hsm.engine") match {
      case "websocket" =>
        new WebSocketScriptRunnerServer(Map())
      case unknown =>
        throw new Exception(s"Unknown server engine '$unknown'")
    }
  }

  val script: List[Script] = List(

  )
}

object Application extends ApplImpl {
  override val config: Config = ConfigFactory.load("application.conf")

  server.run()
}
