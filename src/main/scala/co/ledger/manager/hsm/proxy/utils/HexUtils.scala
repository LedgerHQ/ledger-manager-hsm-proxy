package co.ledger.manager.hsm.proxy.utils

/**
  * A utili
  *
  * User: Pierre Pollastri
  * Date: 07-06-2018
  * Time: 14:28
  *
  */
object HexUtils {
  private val SLIDING_SIZE = 2
  private val SLIDING_STEP = 2
  private val PARSE_RADIX = 16

  def valueOf(array: Array[Byte]): String = array.map("%02X" format _).mkString
  def valueOf(string: String): Array[Byte] =
    string.sliding(SLIDING_SIZE, SLIDING_STEP).toArray.map(Integer.parseInt(_, PARSE_RADIX).toByte)

}