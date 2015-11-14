package wrapper

import java.io.File
import javax.inject.{Inject, Singleton}

import com.braintreegateway.{TransactionRequest, Environment, BraintreeGateway}
import com.typesafe.config.ConfigFactory
import play.api.Play._

import scala.concurrent.ExecutionContext
import java.math.BigDecimal

@Singleton()
class Braintree @Inject()(implicit executionContext: ExecutionContext) {
  val gateway = new BraintreeGateway(
    if (current.configuration.getBoolean("braintree.sandbox").get) Environment.SANDBOX else Environment.PRODUCTION,
    current.configuration.getString("braintree.merchantid").get,
    current.configuration.getString("braintree.publickey").get,
    current.configuration.getString("braintree.privatekey").get
  )

  def getToken = gateway
    .clientToken()
    .generate()

  def doTransaction(amount: Integer, nonce: String) : Option[String] = {
    val result = gateway
      .transaction()
      .sale(
        new TransactionRequest()
          .amount(new BigDecimal(amount))
          .paymentMethodNonce(nonce)
          .options()
            .submitForSettlement(true)
            .done()
      )
    if (result.isSuccess) Some(result.getTarget.getId)
    else None
  }
}
