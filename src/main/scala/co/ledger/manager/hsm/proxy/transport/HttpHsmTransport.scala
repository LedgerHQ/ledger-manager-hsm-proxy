package co.ledger.manager.hsm.proxy.transport

import com.ledger.bluehsm.protobuf.BlueHSMServer.{Request, Response}
import io.lemonlabs.uri.Url
import scalaj.http._

import scala.concurrent.{ExecutionContext, Future}

/**
  * HSM transport over HTTP.
  *
  * User: Pierre Pollastri
  * Date: 25-06-2018
  * Time: 11:59
  *
  */
class HttpHsmTransport(endpoint: Url) extends HsmTransport {
  override def request(request: Request)(implicit ec: ExecutionContext): Future[(Request, Response)] = Future({
    Http(endpoint.toString())
      .postData(request.toByteArray)
      .header("content-type", "application/octet-stream")
      .asBytes
  }).map({ r =>
    if (r.is2xx) r.body else throw HttpException(r.code, r.statusLine)
  }).map(Response.parseFrom).map({ response =>
    (request.copy(id = Some(response.id)), response)
  })
}

case class HttpException(code: Int, status: String) extends Exception(s"$code $status")