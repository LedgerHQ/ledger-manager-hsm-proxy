package co.ledger.manager.hsm.proxy.scripts

import co.ledger.manager.hsm.proxy.{SafeScript, Script}
import co.ledger.manager.hsm.proxy.transport.Transport
import co.ledger.manager.hsm.proxy.utils.ParamsMap
import com.ledger.bluehsm.protobuf.BlueHSMServer.Parameter
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}

/**
  * Describe your class here.
  *
  * User: Pierre Pollastri
  * Date: 26-06-2018
  * Time: 16:43
  *
  */
class UpdateFirmwareScript(override val config: Config) extends SafeScript[UpdateFirmwareScript.UpdateFirmwareScriptParams] with BaseScpScript {
  private val ScriptName = "distributeFirmware11_scan"

  override def parseParams(params: ParamsMap): UpdateFirmwareScript.UpdateFirmwareScriptParams =
    UpdateFirmwareScript.UpdateFirmwareScriptParams(
      params.getIntOrThrow("targetId"),
      params.getStringOrThrow("perso"),
      params.getStringOrThrow("firmwareKey"),
      params.getStringOrThrow("firmware")
    )

  override def run(transport: Transport, params: UpdateFirmwareScript.UpdateFirmwareScriptParams)
                  (implicit ec: ExecutionContext): Future[Any] = {
    openSecureChannel(transport, params.targetId, ScriptName, params.perso) flatMap { channel =>
      def go(channel: SecureChannel): Future[SecureChannel] = {
        if (channel.stream.apdus.isEmpty || channel.stream.apdus.head.isEmpty)
          Future.successful(channel)
        else {
          transport.exchange(channel.stream.apdus.head) flatMap { result =>
            chainQuery(channel, false, Some(Array[Byte](0xFF.toByte, 0xFF.toByte) ++ result.bytes), List())
          } flatMap { c =>
            go(c)
          }
        }
      }
      go(channel)
    } flatMap { channel =>
      chainQuery(channel, true, None, List(
        Parameter(params.firmware, Some("firmware"), Some(false)),
        Parameter(params.firmwareKey, Some("firmwareKey"), Some(false))
      ))
    } flatMap { channel =>
      transport.sendBulk(channel.stream.apdus)
    }
  }
}

object UpdateFirmwareScript {
  case class UpdateFirmwareScriptParams(targetId: Int, perso: String, firmwareKey: String, firmware: String)
}

