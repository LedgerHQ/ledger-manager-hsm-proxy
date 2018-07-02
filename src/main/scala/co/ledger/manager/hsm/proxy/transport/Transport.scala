package co.ledger.manager.hsm.proxy.transport

import scala.concurrent.Future

/**
  * Transport between the runner and the remote device. The class should implement any kind of protocol between the server and
  *  a client.
  *
  * User: Pierre Pollastri
  * Date: 13-06-2018
  * Time: 14:34
  *
  */
trait Transport {

  def id: Int = hashCode()

  /**
    * Send a single APDU command to the remote device, and get the data returned by the device.
    * @param apdu A single APDU command
    * @return A future with the result of the command from the remote device.
    */
  def sendApdu(apdu: Array[Byte]): Future[Array[Byte]]

  /**
    * Send a bulk of APDU commands to the remote device, and get the data from the last command returned by the device.
    * @param bulk A list of APDU commands to run on the remote device
    * @return A future with the result of the last command
    */
  def sendBulk(bulk: List[Array[Byte]]): Future[Array[Byte]]

  /**
    * Send an error message to the client and close the connection.
    * @param errorMessage The error message to send to the client.
    */
  def fail(errorMessage: String): Unit

  /**
    * Send a success message to the client and close the connection.
    */
  def success(): Unit

  /**
    * Send a success message to the client and close the connection.
    * @param message An optional string message to send to the client.
    */
  def success(message: String): Unit

}