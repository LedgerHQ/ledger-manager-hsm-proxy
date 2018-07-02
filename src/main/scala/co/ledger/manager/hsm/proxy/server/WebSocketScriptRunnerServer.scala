package co.ledger.manager.hsm.proxy.server

import java.net.InetSocketAddress

import co.ledger.manager.hsm.proxy.Script
import co.ledger.manager.hsm.proxy.concurrent.ExecutionContext.Implicits.context
import co.ledger.manager.hsm.proxy.transport.WebSocketTransport
import co.ledger.manager.hsm.proxy.utils.ParamsMap
import com.typesafe.scalalogging.LazyLogging
import io.lemonlabs.uri._
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer

/**
  * Describe your class here.
  *
  * User: Pierre Pollastri
  * Date: 07-06-2018
  * Time: 17:50
  *
  */
class WebSocketScriptRunnerServer(hostname: String, port: Int, scripts: Map[String, Script])
  extends ScriptRunnerServer(scripts) with LazyLogging {

  import WebSocketScriptRunnerServer._

  private class Server extends WebSocketServer(new InetSocketAddress(hostname, port)) {
    override def onOpen(conn: WebSocket, handshake: ClientHandshake): Unit = {
      // Create transport
      val transport = new WebSocketTransport(conn)
      conn.setTransport(Some(transport))

      logger.info(s"[${transport.id}] New connection on ${handshake.getResourceDescriptor}")

      // Resolve script
      val uri = RelativeUrl.parse(handshake.getResourceDescriptor)
      resolve(uri.path.toString()) match {
        case Some(script) =>
          logger.info(s"[${transport.id}] Starting script ${uri.path.toString()}")
          conn.setScript(Some(script))
          script(transport, new ParamsMap(uri.query.paramMap.map({case (k, v) => (k, v(0))})))
        case None =>
          logger.error(s"[${transport.id}] Unknown script ${uri.path.toString()}")
          transport.fail(s"Unknown script ${uri.path.toString()}")
      }
    }

    override def onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean): Unit = {
      conn.transport.foreach(t => logger.info(s"[${t.id}] Closing"))
      conn.transport.foreach(_.fail(s"WebSocket closed $code $reason"))
      conn.release()
    }

    override def onMessage(conn: WebSocket, message: String): Unit = conn.transport.foreach(_.receive(message))

    override def onError(conn: WebSocket, ex: Exception): Unit = {
      logger.error("WebSocket error", ex)
      conn.transport.foreach(t => logger.info(s"[${t.id}] ${ex.getMessage}"))
      conn.transport.foreach(_.fail(ex.getMessage))
      conn.release()
    }

    override def onStart(): Unit = {}



  }

  override def run(): Unit = _server.run()

  override def stop(): Unit = _server.stop()

  private val _server = new Server
}

object WebSocketScriptRunnerServer {

  implicit class RichWebSocket(val ws: WebSocket) {
    def transport: Option[WebSocketTransport] = attachment.transport
    def script: Option[Script] = attachment.script
    def setTransport(transport: Option[WebSocketTransport]): Unit = attachment(attachment.copy(transport = transport))
    def setScript(script: Option[Script]): Unit = attachment(attachment.copy(script = script))

    def release(): Unit = attachment(RichWebSocketAttachment(None, None))

    private def attachment: RichWebSocketAttachment =
      if (ws != null && ws.getAttachment[RichWebSocketAttachment] != null)
        ws.getAttachment[RichWebSocketAttachment]
      else
        RichWebSocketAttachment(None, None)

    private def attachment(attachment: RichWebSocketAttachment): Unit = if (ws != null) ws.setAttachment(attachment)
  }

  private case class RichWebSocketAttachment(transport: Option[WebSocketTransport], script: Option[Script])

}