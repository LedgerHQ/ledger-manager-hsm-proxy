package co.ledger.manager.hsm.proxy.tests.utils

import co.ledger.manager.hsm.proxy.ManagerHsmProxy
import com.typesafe.config.{Config, ConfigFactory}

/**
  * Describe your class here.
  *
  * User: Pierre Pollastri
  * Date: 19-06-2018
  * Time: 10:25
  *
  */
class Server {
  private class Internal extends ManagerHsmProxy {
    override val config: Config = ConfigFactory.defaultApplication()
    override def run(): Unit = server.run()
  }


  private val _server = new Internal
  private val _thread = new Thread() {
    override def run(): Unit = {
      super.run()
      _server.run()
    }
  }

  def start(): Unit = _thread.start()
  def stop(): Unit = _server.stop()

}
