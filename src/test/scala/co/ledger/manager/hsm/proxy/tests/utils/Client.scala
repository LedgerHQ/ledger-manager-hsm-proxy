package co.ledger.manager.hsm.proxy.tests.utils

import co.ledger.manager.hsm.proxy.utils.{HexUtils, JSONUtils}
import com.typesafe.config.ConfigFactory
import io.lemonlabs.uri._
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * A small synchronous websocket client
  *
  * User: Pierre Pollastri
  * Date: 15-06-2018
  * Time: 12:03
  *
  */
class Client(url: Url) {
  // Yuck, that class is something not to look at too long.

  def receive(): Message = {
    val start = System.currentTimeMillis()
    def dequeueMessage(): Option[Message] = _messages.synchronized {
      _messages.headOption match {
        case Some(_) => Some(_messages.dequeue())
        case None => None
      }
    }
    def checkMessage(): Future[Message] = Future {
      dequeueMessage() match {
        case Some(message) => Some(message)
        case None =>
          Thread.sleep(20)
          None
      }
    } flatMap {
      case Some(msg) => Future.successful(msg)
      case None =>
        val duration = (System.currentTimeMillis() - start).millisecond
        if (duration > 100000.second)
          Future.failed(new Exception("Timeout"))
        else
          checkMessage()
    }
    Await.result(checkMessage(), Duration.Inf)
  }

  def answer(msg: Message, data: Array[Byte]): Message = {
    val a = msg.respond(response = "success", data = HexUtils.valueOf(data))
    _ws.send(JSONUtils.serialize(a))
    receive()
  }

  def fail(msg: Message, message: String): Message = {
    val a = msg.respond(response = "error", data = message)
    _ws.send(JSONUtils.serialize(a))
    receive()
  }

  def fatalFail(msg: Message, message: String): Message = {
    val a = msg.respond(data = message, response = "fatal_error")
    _ws.send(JSONUtils.serialize(a))
    receive()
  }

  private val _openPromise = Promise[Unit]()
  private val _ws = new WebSocketClient(url.toJavaURI) {
    override def onOpen(handshakedata: ServerHandshake): Unit = {
      println(s"Connected to ${url.toString()}")
      _openPromise.success()
    }

    override def onMessage(message: String): Unit = {
      println(message)
      _messages.synchronized {
        _messages += JSONUtils.deserialize[Message](message)
      }
    }

    override def onClose(code: Int, reason: String, remote: Boolean): Unit =
      println(s"WebSocket closed $code, $reason, $remote")

    override def onError(ex: Exception): Unit = {
      ex.printStackTrace()
      if (!_openPromise.isCompleted) _openPromise.failure(ex)
    }
  }

  def connect(): Future[Unit] = {
    _ws.connect()
    _openPromise.future
  }

  private val _messages = mutable.Queue[Message]()
}

object Client {
  private lazy val config = ConfigFactory.load("application.conf")
  private lazy val hostname = config.getString("server.hostname")
  private lazy val port: Int = config.getInt("server.port")

  def apply(path: String, params: QueryString): Client = new Client(Url(scheme = "http", host = hostname, port = port, query = params, path = path))
}

case class Response(nonce: Int, data: String, response: String)
case class Message(nonce: Option[Int], data: Option[String], query: String) {
  def respond(response: String = query, nonce: Int = Message.this.nonce.get , data: String = Message.this.data.get): Response =
    Response(nonce, data, response)
}