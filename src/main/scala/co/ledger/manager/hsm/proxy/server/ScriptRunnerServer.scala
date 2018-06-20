package co.ledger.manager.hsm.proxy.server

import co.ledger.manager.hsm.proxy.{Dongle, Script}

import scala.concurrent.Future

/**
  * Describe your class here.
  *
  * User: Pierre Pollastri
  * Date: 07-06-2018
  * Time: 14:38
  *
  */
abstract class ScriptRunnerServer(scripts: Map[String, Script]) {

  def run(): Unit
  def stop(): Unit

  def resolve(scriptPath: String): Option[Script] = scripts.get(scriptPath)

}
