package co.ledger.manager.hsm.proxy.transport

import co.ledger.manager.hsm.proxy.utils.JSONUtils
import org.java_websocket.WebSocket

import scala.concurrent.Future

/**
  * Describe your class here.
  *
  * User: Pierre Pollastri
  * Date: 13-06-2018
  * Time: 17:00
  *
  */

class WebSocketTransport(webSocket: WebSocket) extends Transport {


  override def sendApdu(apdu: Array[Byte]): Future[Array[Byte]] = ???

  override def sendBulk(bulk: List[Array[Byte]]): Future[Array[Byte]] = ???

  override def fail(errorMessage: String): Unit = safe() {
    webSocket.send(newErrorMessage(Some(errorMessage)))
    webSocket.close()
  }

  override def success(): Unit = safe()(webSocket.send(newSuccessMessage(None)))


  override def success(message: String): Unit = safe()(webSocket.send(newSuccessMessage(Some(message))))

  def receive(message: String): Unit = {

  }

  private def safe[A](errorResult: => A)(f: => A): A = if (webSocket.isOpen) f else errorResult

  private var _nonce = 0

  private def newErrorMessage(message: Option[String]): NoncelessMessage = NoncelessMessage("error", message)
  private def newSuccessMessage(message: Option[String]): NoncelessMessage = NoncelessMessage("success", message)

  private case class NoncelessMessage(query: String, data: Option[String])
  private case class Message(nonce: Int, query: String, data: String)
  private case class BulkMessage(nonce: Int, query: String, data: Seq[String])

  private implicit class RichWebSocket(val ws: WebSocket) {
    def send[A](obj: A): Unit = ws.send(JSONUtils.serialize(obj))
  }

}
