package co.ledger.manager.hsm.proxy.transport

import scala.concurrent.{ExecutionContext, Future}
import com.ledger.bluehsm.protobuf.BlueHSMServer._
import com.typesafe.config.{Config, ConfigFactory}
import io.lemonlabs.uri.Url

/**
  * HSM Transport
  *
  * User: Pierre Pollastri
  * Date: 25-06-2018
  * Time: 11:57
  *
  */
trait HsmTransport {
  /**
    * Send a request to the HSM and get the response. This call return a tuple with a Request build to chain the next request
    *  and the response of the request.
    * @param request
    * @return
    */
  def request(request: Request)(implicit ec: ExecutionContext): Future[(Request, Response)]
}

object HsmTransport {
  private val HsmEndpointConfigKey = "hsm.endpoint"

  /**
    * Build a default instance of the HSM transport depending on the configuration file.
    * @return
    */
  def apply(config: Config): HsmTransport = new HttpHsmTransport(Url(config.getString(HsmEndpointConfigKey)))
}