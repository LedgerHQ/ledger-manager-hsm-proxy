package co.ledger.manager.hsm.proxy.tests

import co.ledger.manager.hsm.proxy.ManagerHsmProxy
import co.ledger.manager.hsm.proxy.tests.utils.{Client, Message, Server}
import co.ledger.manager.hsm.proxy.utils.HexUtils
import com.typesafe.config.{Config, ConfigFactory}
import io.lemonlabs.uri.QueryString
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Describe your class here.
  *
  * User: Pierre Pollastri
  * Date: 15-06-2018
  * Time: 11:56
  *
  */
class IntegrationTests extends FlatSpec with BeforeAndAfterAll {

  val server = new Server

  it should "echo" in {
    val client = Client("/echo", QueryString.fromPairs())
    client.connect()
    val message = client.receive()
    assert(Message(Some(1), Some("0102"), "exchange") == message)
    val answer =  client.answer(message, HexUtils.valueOf("00"))
    assert(Message(None, Some("00"), "success") == answer)
  }

  it should "echo multiple times" in {
    val client = Client("/echo", QueryString.fromPairs("repeat" -> "3"))
    client.connect()
    val message = client.receive()
    assert(Message(Some(1), Some("0102"), "exchange") == message)
    val answer =  client.answer(message, HexUtils.valueOf("00"))
    assert(Message(None, Some("000000"), "success") == answer)
  }

  it should "failed to echo when messing with nonce" in {
    val client = Client("/echo", QueryString.fromPairs())
    client.connect()
    val message = client.receive()
    assert(Message(Some(1), Some("0102"), "exchange") == message)
    val answer =  client.answer(message.copy(nonce = Some(2)), HexUtils.valueOf("00"))
    assert(answer.query == "error")
  }

  it should "send me back my error in case of fatal error on client side" in {
    val client = Client("/echo", QueryString.fromPairs())
    client.connect()
    val message = client.receive()
    assert(Message(Some(1), Some("0102"), "exchange") == message)
    val errorMessage = "Fatal error system"
    val answer =  client.fatalFail(message, errorMessage)
    assert(answer.query == "error" && answer.data.get == errorMessage)
  }

  it should "send me back SW in case of error on device side" in {
    val client = Client("/echo", QueryString.fromPairs())
    client.connect()
    val message = client.receive()
    assert(Message(Some(1), Some("0102"), "exchange") == message)
    val statusWord = "6a84".toLowerCase
    val answer =  client.fail(message, statusWord)
    assert(answer.query == "error" && answer.data.get.toLowerCase.contains(statusWord))
  }

  override protected def beforeAll(): Unit = server.start()

  override protected def afterAll(): Unit = server.stop()
}

