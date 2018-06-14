package co.ledger.manager.hsm.proxy.concurrent

/**
  * Describe your class here.
  *
  * User: Pierre Pollastri
  * Date: 08-06-2018
  * Time: 14:15
  *
  */
object ExecutionContext {
  object Implicits {
    implicit val context: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  }
}

