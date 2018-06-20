package co.ledger.manager.hsm.proxy.transport

import co.ledger.manager.hsm.proxy.utils.JSONUtils
import org.java_websocket.WebSocket
import co.ledger.manager.hsm.proxy.utils.HexUtils

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

/**
  * Describe your class here.
  *
  * User: Pierre Pollastri
  * Date: 13-06-2018
  * Time: 17:00
  *
  */

class WebSocketTransport(webSocket: WebSocket) extends Transport {
  import WebSocketTransport._

  override def sendApdu(apdu: Array[Byte]): Future[Array[Byte]] = safe(Future.failed[Array[Byte]](UnsafeConnection())) {
    val nonce = newNonce()
    val promise = newQuery(nonce)
    webSocket.send(newApduMessage(nonce, apdu))
    promise.future
  }

  override def sendBulk(bulk: List[Array[Byte]]): Future[Array[Byte]] = ???

  override def fail(errorMessage: String): Unit = safe() {
    webSocket.send(newErrorMessage(Some(errorMessage)))
    webSocket.close()
  }

  override def success(): Unit = safe() {
    webSocket.send(newSuccessMessage(None))
    webSocket.close()
  }

  override def success(message: String): Unit = safe() {
    webSocket.send(newSuccessMessage(Some(message)))
    webSocket.close()
  }

  def receive(message: String): Unit = {
    Try(JSONUtils.deserialize[Message](message)).recover({
      case all: Throwable => throw new Exception(s"Invalid message '$message'")
    }).map({ response =>
      val query = popQuery(response.nonce)
      if (query.isEmpty) throw new Exception(s"Request ${response.nonce} does not exist.")
      if (response.query == "error") throw new Exception(response.data)
      val apdu = response.apdu
      if (apdu.isFailure) throw new Exception(s"Unable to parse APDU data '${response.data}'.")
      query.foreach(_.success(apdu.get))
    }).recover({
      case all: Throwable => fail(all.getMessage)
    })
  }

  private def safe[A](errorResult: => A)(f: => A): A = if (webSocket.isOpen) f else errorResult

  private def newErrorMessage(message: Option[String]): NoncelessMessage = NoncelessMessage("error", message)
  private def newSuccessMessage(message: Option[String]): NoncelessMessage = NoncelessMessage("success", message)
  private def newApduMessage(nonce: Int, apdu: Array[Byte]): Message = {
    Message(nonce, "exchange", HexUtils.valueOf(apdu))
  }

  private def newNonce(): Int = synchronized {
    _lastNonce = _lastNonce + 1
    _lastNonce
  }

  private def newQuery(nonce: Int): Promise[Array[Byte]] = _queries.synchronized {
    val promise = Promise[Array[Byte]]()
    _queries(nonce) = promise
    promise
  }

  private def popQuery(nonce: Int): Option[Promise[Array[Byte]]] = _queries.synchronized {
   _queries.remove(nonce)
  }

  private implicit class RichWebSocket(val ws: WebSocket) {
    def send[A](obj: A): Unit = ws.send(JSONUtils.serialize(obj))
  }

  private var _lastNonce = 0
  private val _queries: mutable.Map[Int, Promise[Array[Byte]]] = mutable.Map()
}

case class UnsafeConnection() extends Exception("The connection is currently unsafe to used")

object WebSocketTransport {
  case class NoncelessMessage(query: String, data: Option[String])
  case class Message(nonce: Int, query: String, data: String) {
    def apdu: Try[Array[Byte]] = Try(HexUtils.valueOf(data))
  }
  case class BulkMessage(nonce: Int, query: String, data: Seq[String])
}