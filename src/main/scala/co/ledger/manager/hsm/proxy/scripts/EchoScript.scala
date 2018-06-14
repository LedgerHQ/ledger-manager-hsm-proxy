package co.ledger.manager.hsm.proxy.scripts

import co.ledger.manager.hsm.proxy.{SafeScript, Script}
import co.ledger.manager.hsm.proxy.transport.Transport
import co.ledger.manager.hsm.proxy.utils.HexUtils

import scala.concurrent.{ExecutionContext, Future}

/**
  * Describe your class here.
  *
  * User: Pierre Pollastri
  * Date: 14-06-2018
  * Time: 14:48
  *
  */


class EchoScript extends SafeScript[EchoParams] {
  override def parseParams(params: Map[String, String]): EchoParams = {
    EchoParams(params.get("int").map(_.toInt))
  }

  override def run(transport: Transport, params: EchoParams)(implicit ec: ExecutionContext): Future[Any] =
    transport.exchange(Array[Byte](0x01.toByte, 0x02)) map { r =>
      transport.success(HexUtils.valueOf(r.bytes))
    }
}

case class EchoParams(p: Option[Int])