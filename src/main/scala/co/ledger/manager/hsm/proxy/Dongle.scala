package co.ledger.manager.hsm.proxy

import scala.concurrent.Future

/**
  * Base trait for a transport method for a hardware device.
  *
  * User: Pierre Pollastri
  * Date: 07-06-2018
  * Time: 14:06
  *
  */
trait Dongle {
  def exchange(): Future[Unit]
}
