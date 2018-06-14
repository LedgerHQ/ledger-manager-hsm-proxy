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

trait Script {
  implicit class RichTransport(val transport: Transport) {
    def exchange(apdu: Array[Byte])(implicit ec: ExecutionContext): Future[BytesReader] = transport.sendApdu(apdu).map(BytesReader.apply)
    def bulkExchange(bulk: List[Array[Byte]])(implicit ec: ExecutionContext): Future[BytesReader] = transport.sendBulk(bulk).map(BytesReader.apply)
  }

  def apply(transport: Transport, params: Map[String, String])(implicit ec: ExecutionContext): Unit
}

trait SafeScript[Params] extends Script {

  def parseParams(params: Map[String, String]): Params

  def run(transport: Transport, params: Params)(implicit ec: ExecutionContext): Future[Any]

  override def apply(transport: Transport, params: Map[String, String])(implicit ec: ExecutionContext): Unit = {
    Future.fromTry(Try(parseParams(params))) flatMap { p =>
      run(transport, p)
    } recover {
      case all: Throwable => transport.fail(all.getMessage)
    }
  }
}