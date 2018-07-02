package co.ledger.manager.hsm.proxy

/**
  * Describe your class here.
  *
  * User: Pierre Pollastri
  * Date: 26-06-2018
  * Time: 17:27
  *
  */
package object exceptions {

  case class MissingKeyException(key: String) extends Exception(s"Missing key '$key' in script params")
  sealed abstract class WrongTypeException(key: String, typename: String) extends Exception(s"'$key' cannot be interpreted as $typename")
  case class ExpectedIntException(key: String) extends WrongTypeException(key, "int")
}
