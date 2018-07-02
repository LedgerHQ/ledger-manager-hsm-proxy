package co.ledger.manager.hsm.proxy.scripts

import co.ledger.manager.hsm.proxy.Script
import co.ledger.manager.hsm.proxy.transport.HsmTransport
import co.ledger.manager.hsm.proxy.utils.{BytesReader, BytesWriter, HexUtils}
import com.google.protobuf.ByteString
import com.twitter.io.ByteWriter
import com.ledger.bluehsm.protobuf.BlueHSMServer.{Parameter, Request, Response}
import com.typesafe.config.Config

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

/**
  * Describe your class here.
  *
  * User: Pierre Pollastri
  * Date: 20-06-2018
  * Time: 12:07
  *
  */
trait BaseScpScript { self: Script  =>

  val config: Config

  def identifyDevice(transport: RichTransport, targetId: Int)(implicit ec: ExecutionContext): Future[Int] = {
    transport.exchange(
      new BytesWriter()
        .writeByteArray(Array(0xe0.toByte, 0x04, 0x00, 0x00))
        .writeByte(4).writeInt(targetId).toByteArray
    ).map(_ => targetId)
  }

  def openSecureChannel(transport: RichTransport, targetId: Int, scriptReference: String, persoKey: String)
                       (implicit ec: ExecutionContext): Future[SecureChannel] ={
    identifyDevice(transport, targetId).flatMap({ _ =>
      getEphemeralKey(transport, targetId, scriptReference, persoKey)
    }).flatMap({ key =>
      getRemoteCertificate(transport, key)
    }).flatMap({ cert =>
      openSecureChannel(transport, cert)
    })
  }

  def getEphemeralKey(transport: RichTransport, targetId: Int, scriptReference: String, persoKey: String)
                     (implicit ec: ExecutionContext)
  : Future[EphemeralKey] = {
    query(newScpQuery(targetId, scriptReference, persoKey)).map({case (next, response) =>
        val reader = new BytesReader(response.response.get.toByteArray)
        val remotePublicKey = reader.readNextBytes(65)
        val nonce = reader.readNextBytes(8)
        val scpv2 = isUsingScpv2(targetId)
        val masterPublicKey = {
          if (scpv2)
            Some(reader.readNextBytes(65))
          else
            None
        }
        val masterPublicKeySignature = {
          if (scpv2)
            Some(reader.readNextBytes(reader.peekByte(1) + 2 ))
          else
            None
        }
        EphemeralKey(next, remotePublicKey, nonce, masterPublicKey,
          masterPublicKeySignature, scpv2, targetId, scriptReference,
          persoKey
        )
    })
  }

  def getRemoteCertificate(transport: RichTransport, ephemeralKey: EphemeralKey)
                          (implicit ec: ExecutionContext): Future[RemoteCertificate] = {
    // Initialize the chain
    transport.exchange(Array[Byte](0xE0.toByte, 0x50, 0x00, 0x00, 0x08) ++ ephemeralKey.nonce) flatMap { reader =>
      // Get remote certificate
      reader.seek(4)
      val deviceNonce = reader.readNextBytes(8)
      query(ephemeralKey.next.copy(
        remoteParameters = persoKeyParameters(ephemeralKey.persoKey),
        parameters = Some(ByteString.copyFrom(deviceNonce))
      )) map { case (next, response) =>
          val reader = new BytesReader(response.response.get.toByteArray)
          val remotePublickKeySignature = reader.readNextBytes(reader.peekByte(1) + 2)
          val certificate = new BytesWriter()
            .writeByte(ephemeralKey.remotePublicKey.length)
            .writeByteArray(ephemeralKey.remotePublicKey)
            .writeByte(remotePublickKeySignature.length)
            .writeByteArray(remotePublickKeySignature).toByteArray
          RemoteCertificate(next, ephemeralKey, deviceNonce, remotePublickKeySignature, certificate)
      }
    }
  }

  def openSecureChannel(transport: RichTransport, remoteCertificate: RemoteCertificate)(implicit ec: ExecutionContext): Future[SecureChannel] = {
    transport.exchange(
      Array[Byte](0xE0.toByte, 0x51, 0x80.toByte, 0x00) ++
      Array[Byte](remoteCertificate.certificate.length.toByte) ++
      remoteCertificate.certificate) flatMap { _ =>
      // Walk the chain
      def walk(index: Int, next: Request): Future[Request] = {
        if (index > 1)
          Future.successful(next)
        else {
          val apdu = Array[Byte](0xE0.toByte, 0x52, if (index == 0) 0x00 else 0x80.toByte, 0x00, 0x00)
          transport.exchange(apdu) flatMap { certificate =>
            if (certificate.length == 0)
              Future.successful(next)
            else {
              query(next.copy(parameters = Some(ByteString.copyFrom(certificate.bytes)))) flatMap {case (request, response) =>
                walk(index + 1, request)
              }
            }
          }
        }

      }
      walk(0, remoteCertificate.next)
    } flatMap { next =>
      // Commit agreement
      transport.exchange(Array[Byte](0xE0.toByte, 0x53, 0x00, 0x00, 0x00)).map(_ => next)
    } flatMap { next =>
      query(next.copy(remoteParameters = secureChannelParameters(remoteCertificate.ephemeralKey), parameters = None))
    } map { case (request, response) =>
      SecureChannel(request, remoteCertificate, ApduStream(List(response.response.get.toByteArray)))
    }
  }

  def newScpQuery(targetId: Int, scriptReference: String, persoKey: String): Request = {
    val params = persoKeyParameters(persoKey) ++ secureChannelParameters(targetId)
    Request(
      reference = Some(scriptReference),
      remoteParameters = params,
      largeStack = Some(true)
    )
  }
  def persoKeyParameters(persoKey: String): List[Parameter] =
    List(Parameter(local = Some(false), alias = Some("persoKey"), name = persoKey))

  def secureChannelParameters(useScpv2: Boolean): List[Parameter] =
    if (useScpv2) List(Parameter(local = Some(false), alias = Some("scpv2"), name = "dummy")) else List()
  def secureChannelParameters(targetId: Int): List[Parameter] =
    secureChannelParameters(isUsingScpv2(targetId))
  def secureChannelParameters(ephemeralKey: EphemeralKey): List[Parameter] =
    secureChannelParameters(ephemeralKey.targetId)



  def isUsingScpv2(targetId: Int): Boolean = (targetId & 0x0F) >= 0x03

  def query(query: Request)(implicit ec: ExecutionContext): Future[(Request, Response)] = hsmTransport.request(query)
  def chainQuery(channel: SecureChannel, splitApdu: Boolean, parameters: Option[Array[Byte]] = None, remoteParameters: List[Parameter])
                (implicit ec: ExecutionContext): Future[SecureChannel] = {
    query(channel.next.copy(parameters = parameters.map(ByteString.copyFrom), remoteParameters = remoteParameters))
      .map({ case (next, response) =>
        channel.copy(
          next = next,
          stream = channel.stream.pushOnTop(response.response.map(_.toByteArray).map(splitApdus(splitApdu)).getOrElse(List())))
    })
  }

  private def splitApdus(enable: Boolean)(response: Array[Byte]): List[Array[Byte]] = {
    if (enable) {
      val reader = new BytesReader(response)
      @tailrec
      def go(out: List[Array[Byte]]): List[Array[Byte]] = {
        if (reader.available == 0)
          out
        else {
          go(out :+ reader.readNextBytes(reader.peekByte(4) + 5))
        }
      }
      go(List())
    } else {
      List(response)
    }
  }

  lazy val hsmTransport: HsmTransport = HsmTransport(config)
}

case class EphemeralKey(next: Request, remotePublicKey: Array[Byte], nonce: Array[Byte],
                        masterPublicKey: Option[Array[Byte]], masterPublicKeySignature: Option[Array[Byte]],
                        isUsingScpv2: Boolean,
                        targetId: Int, referenceName: String, persoKey: String)

case class RemoteCertificate(next: Request, ephemeralKey: EphemeralKey, deviceNonce: Array[Byte],
                             remotePublicKeySignature: Array[Byte], certificate: Array[Byte])

case class SecureChannel(next: Request, remoteCertificate: RemoteCertificate, stream: ApduStream)

trait ApduStream {
  def apdus: List[Array[Byte]]
  def next: ApduStream
  def isEOF: Boolean
  def pushOnTop(apdus: List[Array[Byte]]): ApduStream = ApduStreamNode(apdus, this)
}

case class ApduStreamNode(override val apdus: List[Array[Byte]], override val next: ApduStream) extends ApduStream {
  override def isEOF: Boolean = false
}

object ApduStream {
  val EOF: ApduStream = new ApduStream {
    override def apdus: List[Array[Byte]] = List()
    override def next: ApduStream = this
    override def isEOF: Boolean = true
  }

  def apply(apdus: List[Array[Byte]]): ApduStream = ApduStreamNode(apdus, EOF)

}
