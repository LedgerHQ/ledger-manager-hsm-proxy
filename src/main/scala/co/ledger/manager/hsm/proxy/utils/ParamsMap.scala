package co.ledger.manager.hsm.proxy.utils

import co.ledger.manager.hsm.proxy.exceptions.{ExpectedIntException, MissingKeyException}

import scala.util.Try

/**
  * Describe your class here.
  *
  * User: Pierre Pollastri
  * Date: 26-06-2018
  * Time: 17:28
  *
  */
class ParamsMap(map: Map[String, String]) {

  def getString(key: String): Option[String] = map.get(key)

  def getStringOrThrow(key: String): String = {
    map.getOrElse(key, throw MissingKeyException(key))
  }

  def getInt(key: String): Option[Int] = getString(key).map(_.toInt)

  def getIntOrThrow(key: String): Int = {
    val str = getStringOrThrow(key)
    Try(str.toInt).getOrElse(throw ExpectedIntException(key))
  }



}
