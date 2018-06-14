package co.ledger.manager.hsm.proxy.utils

import com.google.protobuf.ByteString
import com.twitter.finatra.json.FinatraObjectMapper

/**
  * Utility methods to work with JSON
  *
  * User: Pierre Pollastri
  * Date: 24-01-2018
  * Time: 18:49
  *
  */
object JSONUtils {
  private val mapper = FinatraObjectMapper.create()

  def serialize[A](obj: A): String = mapper.writeValueAsString(obj)
  def deserialize[A](str: String)(implicit manifest: Manifest[A]): A = mapper.parse[A](str)
  def deserialize[A](b: ByteString)(implicit manifest: Manifest[A]): A = deserialize(b.toStringUtf8)


}