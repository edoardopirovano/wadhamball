package controllers

import java.sql.{Date, Timestamp}
import javax.inject.Inject

import dao.TicketDAO
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import views.html
import wrapper.{Mailer, Braintree}

import scala.concurrent.Future

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class Deposit @Inject() (ticketDAO: TicketDAO, mailer: Mailer, val braintree: Braintree, val messagesApi: MessagesApi) extends Controller with I18nSupport {

  val depositPrice = 65

  val transactionFee = 3

  val diningFee = 45

  val emailSubject = "Ticket Deposit Confirmation"

  val allPaidEmailSubject = "Ticket Confirmation"

  val emailText = StringContext("Hello ", ",<br /><br />" +
    "Thank you for placing your deposit for a ticket to Wadham Ball 2016! Your ticket reference is ", ". " +
    "We'll be in touch once you need to pay the remaining £65 of the ticket price (this will be the week before the main ticket sale).<br /><br />" +
    "If you have any questions, feel free to send an email to <a href='mailto:ball.president@wadh.ox.ac.uk'>ball.president@wadh.ox.ac.uk</a> for help.<br /><br />" +
    "Best regards,<br />" +
    "Wadham Ball Committee")

  val allPaidEmailText = StringContext("Hello ", ",<br /><br />" +
    "Thank you for paying the remaining balance for your ticket to Wadham Ball 2016! Your ticket reference is ", ". " +
    "We look forward to seeing you at Wadham Ball 2016.<br /><br />" +
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

  val settleForm = Form(
    mapping(
      "id" -> longNumber(0),
      "diningUpgrade" -> boolean,
      "payment_method_nonce" -> nonEmptyText
    )(SettleForm.apply)(SettleForm.unapply))

  val deposit = Action.async { implicit rs =>
    Future { Ok(html.deposit(depositForm, braintree.getToken, false)) }
  }

//  def doDeposit = Action.async { implicit rs =>
//    depositForm.bindFromRequest.fold(
//      formWithErrors => Future { BadRequest(html.deposit(formWithErrors, braintree.getToken, false)) },
//      depositRequest => {
//        braintree.doTransaction(depositPrice, depositRequest.payment_method_nonce) match {
//          case Some(transactionId) =>
//            val ticketId = Await.result(ticketDAO.insert(new Ticket(None, depositRequest.firstName, depositRequest.lastName, depositRequest.email, Some(transactionId), None, Some(false))), Duration.Inf)
//            mailer.sendMail(Seq(depositRequest.email), emailSubject, emailText.s(depositRequest.firstName, ticketId), unsub = false)
//            Future { Ok(html.didDeposit()) }
//          case None => Future { BadRequest(html.deposit(depositForm.withError("failedPayment", ""), braintree.getToken, true)) }
//        }
//      })
//  }

  def settle(id: Int) = Action.async { implicit rs =>
    Future { Ok(getSettlePage(id, false)) }
  }

  private def getSettlePage(id: Long, failed: Boolean)(implicit rs: Request[AnyContent]) = {
    val diningAvail = ticketDAO.diningAvailable
    val name = ticketDAO.getName(id)
    val alreadyPaid = ticketDAO.hasPaid(id)
    html.settle(settleForm, braintree.getToken, failed, Await.result(name, Duration.Inf), Await.result(diningAvail, Duration.Inf), Await.result(alreadyPaid, Duration.Inf), id)
  }

  def doSettle = Action.async { implicit rs =>
    settleForm.bindFromRequest.fold(
      formWithErrors => Future { BadRequest(getSettlePage(settleForm.get.id, false)) },
      settleRequest => {
        var toPay = depositPrice + transactionFee
        if (settleRequest.diningUpgrade) toPay += diningFee
        var error = false
        val diningAvail = ticketDAO.diningAvailable
        val alreadyPaid = ticketDAO.hasPaid(settleRequest.id)
        if (Await.result(alreadyPaid, Duration.Inf) || (!Await.result(diningAvail, Duration.Inf) && settleRequest.diningUpgrade)) error = true
        else braintree.doTransaction(toPay, settleRequest.payment_method_nonce) match {
          case Some(transactionId) =>
            val name = ticketDAO.getName(settleRequest.id)
            val emailTo = ticketDAO.getEmail(settleRequest.id)
            val paymentAdd = ticketDAO.addPayment(settleRequest.id, transactionId)
            if (settleRequest.diningUpgrade) {
              val addDining = ticketDAO.makeDining(settleRequest.id)
              Await.result(addDining, Duration.Inf)
            }
            Await.result(paymentAdd, Duration.Inf)
            mailer.sendMail(Seq(Await.result(emailTo, Duration.Inf).get), allPaidEmailSubject, allPaidEmailText.s(Await.result(name, Duration.Inf).get, settleRequest.id), unsub = false)
          case None => error = true
        }
        error match {
          case true => Future { BadRequest(getSettlePage(settleRequest.id, true)) }
          case false => Future { Ok(html.paid()) }
        }
      })
  }

}