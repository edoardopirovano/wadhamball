package wrapper

import java.io.File
import javax.inject.{Inject, Singleton}

import com.braintreegateway.{TransactionRequest, Environment, BraintreeGateway}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext
import java.math.BigDecimal

@Singleton()
class Braintree @Inject()(implicit executionContext: ExecutionContext) {
  val config = ConfigFactory.parseFile(new File("conf/payment.conf")).resolve()
  val gateway = new BraintreeGateway(
    Environment.SANDBOX,
    config.getString("merchantid"),
    config.getString("publickey"),
    config.getString("privatekey")
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
