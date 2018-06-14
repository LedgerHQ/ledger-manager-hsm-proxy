package co.ledger.manager.hsm.proxy.proxy

import java.net.InetSocketAddress

import co.ledger.manager.hsm.proxy.Script
import co.ledger.manager.hsm.proxy.server.ScriptRunnerServer
import co.ledger.manager.hsm.proxy.transport.WebSocketTransport
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import io.lemonlabs.uri._
import co.ledger.manager.hsm.proxy.concurrent.ExecutionContext.Implicits.context

import scala.None
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}

/**
  * Describe your class here.
  *
  * User: Pierre Pollastri
  * Date: 07-06-2018
  * Time: 17:50
  *
  */
class WebSocketScriptRunnerServer(scripts: Map[String, Script]) extends ScriptRunnerServer(scripts) {
  import WebSocketScriptRunnerServer._

  private class Server extends WebSocketServer(new InetSocketAddress("localhost", 3000)) {
    override def onOpen(conn: WebSocket, handshake: ClientHandshake): Unit = {
      // Create transport
      val transport = new WebSocketTransport(conn)
      conn.setTransport(Some(transport))

      // Resolve script
      val uri = RelativeUrl.parse(handshake.getResourceDescriptor)
      resolve(uri.path.toString()) match {
        case Some(script) =>
          conn.setScript(Some(script))
        case None =>
          Future(transport.fail(s"Unknown script ${uri.path.toString()}"))
      }
    }

    override def onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean): Unit =
        conn.transport.foreach(_.fail(s"WebSocket closed $code $reason"))

    override def onMessage(conn: WebSocket, message: String): Unit = conn.transport.foreach(_.receive(message))

    override def onError(conn: WebSocket, ex: Exception): Unit = conn.transport.foreach(_.fail(ex.getMessage))

    override def onStart(): Unit = {}



  }

  override def run(): Unit = {
    new Server().run()
  }
}

object WebSocketScriptRunnerServer {

  implicit class RichWebSocket(val ws: WebSocket) {
    def transport: Option[WebSocketTransport] = attachment.transport
    def script: Option[Script] = attachment.script
    def setTransport(transport: Option[WebSocketTransport]): Unit = attachment(attachment.copy(transport = transport))
    def setScript(script: Option[Script]): Unit = attachment(attachment.copy(script = script))



    private def attachment: RichWebSocketAttachment =
      if (ws != null && ws.getAttachment[RichWebSocketAttachment] != null)
        ws.getAttachment[RichWebSocketAttachment]
      else
        RichWebSocketAttachment(None, None)

    private def attachment(attachment: RichWebSocketAttachment): Unit = ws.setAttachment(attachment)
  }

  private case class RichWebSocketAttachment(transport: Option[WebSocketTransport], script: Option[Script])

}