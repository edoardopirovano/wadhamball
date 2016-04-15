package controllers

import java.sql.Timestamp
import javax.inject.Inject

import dao.EmailsDAO
import models.Email
import play.api._
import play.api.data.Form
import play.api.data.Forms.{email, mapping}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, Controller}
import views.html
import wrapper.{Braintree, Mailer}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/** Manage a database of computers. */
class Payment @Inject() (braintree: Braintree, val messagesApi: MessagesApi) extends Controller with I18nSupport {

  def pay = Action.async{ implicit rs =>
    Future { Ok(html.payment(braintree.getToken)) }
  }

  def checkout(payment_method_nonce: String) = Action.async{ implicit rs =>
    braintree.doTransaction(100, payment_method_nonce)
    Future { Ok(html.payment(braintree.getToken)) }
  }

  def paid = Action.async{ implicit rs =>
    Future { Ok(html.paid()) }
  }
}
