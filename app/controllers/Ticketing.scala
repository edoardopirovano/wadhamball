package controllers;

import java.sql.{Date, Timestamp}
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

import dao.TicketDAO
import models._
import play.api.Logger
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

class Ticketing @Inject() (ticketDAO: TicketDAO, mailer: Mailer, val braintree: Braintree, val messagesApi: MessagesApi) extends Controller with I18nSupport {
  val ticketPrice = 133

  val maxTicketsPerPerson = 3

  val diningFee = 46

  val maxDining = 138

  val maxTickets = 1138

  val currentlyProcessing = new AtomicInteger()
  val currentlyProcessingDining = new AtomicInteger()

  val allPaidEmailSubject = "Ticket Confirmation"

  val ticketBoughtEmailText = StringContext("Hello ", ",<br /><br />" +
    "Thank you for buying your ticket to Wadham Ball 2016! Your ticket reference is ", ". " +
    "We look forward to seeing you at Wadham Ball 2016.<br /><br />" +
    "If you have any questions, feel free to send an email to <a href='mailto:ball.president@wadh.ox.ac.uk'>ball.president@wadh.ox.ac.uk</a> for help.<br /><br />" +
    "Best regards,<br />" +
    "Wadham Ball Committee")
//"This email has already been used to pay a deposit",
  val buyForm = Form(
    mapping(
      "noOfTickets" -> number(min=1, max=maxTicketsPerPerson),
      "firstNames" -> seq(nonEmptyText(1, 50)),
      "lastNames" -> seq(nonEmptyText(1, 50)),
      "emails" -> seq(email.verifying("This email has already been used to buy a ticket", email => !Await.result(ticketDAO.contains(email), Duration.Inf))),
      "diningUpgrade0" -> default(boolean, false),
      "diningUpgrade1" -> default(boolean, false),
      "diningUpgrade2" -> default(boolean, false),
      "donation" -> default(number(min=0, max=100), 0),
      "payment_method_nonce" -> nonEmptyText
    )(BuyForm.apply)(BuyForm.unapply))


  private def getBuy(form: Form[BuyForm], failed: Option[String])(implicit rs: Request[AnyContent]) = {
    val dining = ticketDAO.diningCount
    val tickets = ticketDAO.count
    html.tickets(form, braintree.getToken, failed, (currentlyProcessingDining.get() + Await.result(dining, Duration.Inf)) <= maxDining, maxTicketsPerPerson, (currentlyProcessing.get() + Await.result(tickets, Duration.Inf)) > maxTickets)
  }

  def buy = Action.async { implicit rs =>
    Future { Ok(getBuy(buyForm, None)) }
  }

  def doBuy = Action.async { implicit rs =>
    buyForm.bindFromRequest.fold(
      formWithErrors => Future { BadRequest(getBuy(formWithErrors, None)) },
      buyRequest => {
        // Calculate price
        var toPay = ticketPrice * buyRequest.noOfTickets
        var dining = 0
        val diningUpgrades = Seq(buyRequest.diningUpgrade0, buyRequest.diningUpgrade1, buyRequest.diningUpgrade2)
        diningUpgrades.foreach(if (_) dining += 1)
        toPay += dining * diningFee
        toPay += buyRequest.donation

        // Verify tickets available in (maybe) thread-safe way
        var error:Option[String] = None
        val currentlySelling = currentlyProcessing.addAndGet(buyRequest.noOfTickets)
        val currentlySellingDining = currentlyProcessingDining.addAndGet(dining)
        val soldTicketCount = ticketDAO.count
        val soldDiningCount = ticketDAO.diningCount
        if ((currentlySellingDining + Await.result(soldDiningCount, Duration.Inf)) > maxDining)
          error = Some("Not enough dining tickets available to fulfill your request currently.")
        if ((currentlySelling + Await.result(soldTicketCount, Duration.Inf)) > maxTickets)
          error = Some("Not enough tickets available to fulfill your request currently.")
        if ((buyRequest.firstNames.length != buyRequest.noOfTickets) || (buyRequest.lastNames.length != buyRequest.noOfTickets) || (buyRequest.firstNames.length != buyRequest.noOfTickets))
          error = Some("An error occurred processing your input, please ensure you fill in every field correctly.")
        if(System.currentTimeMillis() < 1455642000000L)
          error = Some("There are a limited number of tickets still available for Wadham Ball, the final ticket release will go live on Tuesday of 5th week (16th February) at 5pm.")

        // Perform transaction and update database
        if (error.isEmpty) braintree.doTransaction(toPay, buyRequest.payment_method_nonce) match {
            case Some(transactionId) =>
              for (ticket <- 0 until buyRequest.noOfTickets) {
                val ticketId = Await.result(ticketDAO.insert(new Ticket(None, buyRequest.firstNames(ticket), buyRequest.lastNames(ticket), buyRequest.emails(ticket), None, Some(transactionId), Some(diningUpgrades(ticket)), if (ticket == 0) buyRequest.donation else 0)), Duration.Inf)
                mailer.sendMail(Seq(buyRequest.emails(ticket)), allPaidEmailSubject, ticketBoughtEmailText.s(buyRequest.firstNames(ticket), ticketId), unsub = false)
                  .map((success: Boolean) => if (!success) Logger.error("Email failed to send to " + buyRequest.emails(ticket)))
              }
            case None => error = Some("Payment processing failed, please check your card details.")
          }

        // Tickets now accounted for in database so can decrement here
        currentlyProcessing.addAndGet(- buyRequest.noOfTickets)
        currentlyProcessingDining.addAndGet(- dining)
        error match {
          case Some(_) => Future { BadRequest(getBuy(buyForm, error)) }
          case None => Future { Ok(html.paid()) }
        }
      })
  }
}

