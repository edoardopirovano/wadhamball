package controllers

import java.sql.{Date, Timestamp}
import javax.inject.Inject

import dao.TicketDAO
import models.{DepositForm, Registration, Email}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.{Result, AnyContent, Action, Controller}
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import views.html
import wrapper.Braintree

import scala.concurrent.Future

import models.Ticket

class Deposit @Inject() (ticketDAO: TicketDAO, val braintree: Braintree, val messagesApi: MessagesApi) extends Controller with I18nSupport {

  val depositForm = Form(
    mapping(
      "firstName" -> nonEmptyText(1, 50),
      "lastName" -> nonEmptyText(1, 50),
      "email" -> email.verifying("Must be a Wadham email address", email => email.contains("@wadh.ox.ac.uk")),
      "payment_method_nonce" -> nonEmptyText
    )(DepositForm.apply)(DepositForm.unapply))

  val deposit = Action.async { implicit rs =>
    Future { Ok(html.deposit(depositForm, braintree.getToken)) }
  }

  def doDeposit = Action.async { implicit rs =>
    depositForm.bindFromRequest.fold(
      formWithErrors => Future { BadRequest(html.deposit(formWithErrors, braintree.getToken)) },
      depositRequest => {
        if (!braintree.doTransaction(100, depositRequest.payment_method_nonce)) {
          Future {
            BadRequest(html.deposit(depositForm.withError("failedPayment", "Payment failed to execute."), braintree.getToken))
          }
        } else {
          ticketDAO.insert(new Ticket(depositRequest.firstName, depositRequest.lastName, depositRequest.email, true, new Timestamp(1456790400)))
          Future {
            Ok(html.didDeposit())
          }
        }
      })
  }

}