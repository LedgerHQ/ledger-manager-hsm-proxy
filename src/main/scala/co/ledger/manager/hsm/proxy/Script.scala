package co.ledger.manager.hsm.proxy

import co.ledger.manager.hsm.proxy.transport.Transport
import co.ledger.manager.hsm.proxy.utils.{BytesReader, JSONUtils}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

/**
  * A script to run with a dongle proxy, to return a JSON formatted result
  *
  * User: Pierre Pollastri
  * Date: 07-06-2018
  * Time: 14:06
  *
  */
abstract class RunningScript(val dongle: Dongle) {

  def exchange(apdu: Array[Byte]): Future[BytesReader] = {
    null
  }

  def bulkExchange(bulk: List[Array[Byte]])

  def success[A](result: A): Unit = _promise.tryComplete(Try(JSONUtils.serialize(result)))
  def fail(ex: Throwable): Unit = _promise.failure(ex)
  def future: Future[String] = _promise.future

  private val _promise = Promise[String]()
}

trait Script {
  def apply(transport: Transport, params: Map[String, String])(implicit ec: ExecutionContext): RunningScript
}