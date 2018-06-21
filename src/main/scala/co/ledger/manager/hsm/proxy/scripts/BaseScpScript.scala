package co.ledger.manager.hsm.proxy.scripts

import co.ledger.manager.hsm.proxy.Script

import scala.concurrent.{ExecutionContext, Future}

/**
  * Describe your class here.
  *
  * User: Pierre Pollastri
  * Date: 20-06-2018
  * Time: 12:07
  *
  */
trait BaseScpScript { self: Script  =>

  def identifyDevice(targetId: Int)(implicit ec: ExecutionContext): Future[Int] = {
    ???
  }

  def getEphemeralKey(targetId: Int, script: Option[String], perso: Option[String]): Future[EphemeralKey] = ???
  def getAgreement(ephemeralKey: EphemeralKey): Future[Agreement] = ???
  def commitOnAgreement(agreement: Agreement): Future[SecureCommands] = ???

  def query(query: HsmQuery): Future[HsmResponse] = ???
}

case class HsmQuery()
case class HsmResponse()
case class EphemeralKey()
case class Agreement()
case class SecureCommands()
