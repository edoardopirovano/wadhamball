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
import wrapper.{Mailer, Braintree}

import scala.concurrent.Future

import models.Ticket

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class Deposit @Inject() (ticketDAO: TicketDAO, mailer: Mailer, val braintree: Braintree, val messagesApi: MessagesApi) extends Controller with I18nSupport {

  val depositPrice = 65

  val emailSubject = "Ticket Deposit Confirmation"

  val emailText = StringContext("Hello ", ",<br /><br />" +
    "Thank you for placing your deposit for a ticket to Wadham Ball 2016! Your ticket reference is ", ". " +
    "We'll be in touch once you need to pay the remaining £65 of the ticket price (this will be the week before the main ticket sale, probably second week of Hilary).<br /><br />" +
    "If you have any questions, feel free to send an email to <a href='mailto:ball.president@wadh.ox.ac.uk'>ball.president@wadh.ox.ac.uk</a> for help.<br /><br />" +
    "Best regards,<br />" +
    "Wadham Ball Committee")

  val depositForm = Form(
    mapping(
      "firstName" -> nonEmptyText(1, 50),
      "lastName" -> nonEmptyText(1, 50),
      "email" -> email
        .verifying("Must be a Wadham email address", email => email.contains("@wadh.ox.ac.uk"))
        .verifying("This email has already been used to pay a deposit", email => !Await.result(ticketDAO.contains(email), Duration.Inf)),
      "payment_method_nonce" -> nonEmptyText
    )(DepositForm.apply)(DepositForm.unapply))

  val deposit = Action.async { implicit rs =>
    Future { Ok(html.deposit(depositForm, braintree.getToken)) }
  }

  def doDeposit = Action.async { implicit rs =>
    depositForm.bindFromRequest.fold(
      formWithErrors => Future { BadRequest(html.deposit(formWithErrors, braintree.getToken)) },
      depositRequest => {
        braintree.doTransaction(depositPrice, depositRequest.payment_method_nonce) match {
          case Some(transactionId) =>
            val ticketId = Await.result(ticketDAO.insert(new Ticket(None, depositRequest.firstName, depositRequest.lastName, depositRequest.email, Some(transactionId), None)), Duration.Inf)
            mailer.sendMail(Seq(depositRequest.email), emailSubject, emailText.s(depositRequest.firstName, ticketId), unsub = false)
            Future { Ok(html.didDeposit()) }
          case None => Future { BadRequest(html.deposit(depositForm.withError("failedPayment", "Payment failed to execute."), braintree.getToken)) }
        }
      })
  }

}