package controllers

import java.sql.Timestamp
import javax.inject.Inject

import dao.{TicketDAO, EmailsDAO}
import models.{Email, _}
import org.h2.jdbc.JdbcSQLException
import play.api._
import play.api.data.Form
import play.api.data.Forms.{email, mapping, _}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, Controller}
import views.html
import wrapper.Mailer

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/** Manage a database of computers. */
class Application @Inject() (emailsDAO: EmailsDAO, ticketDAO: TicketDAO, mailer: Mailer, val messagesApi: MessagesApi) extends Controller with I18nSupport {

  val Home = Redirect(routes.Application.home())
  val Subscribe = Redirect(routes.Application.subscription())
  val EmailSend = Redirect(routes.Application.sendmail())
  val ReminderSend = Redirect(routes.Application.sendreminder())

  val emailForm = Form(
    mapping(
      "email" -> email
    )
      ((x:String) => new Email(x, new Timestamp(System.currentTimeMillis())))
      ((x:Email) => Option(x.email)))

  val emailSendForm = Form(
    mapping(
      "subject" -> nonEmptyText(1, 100),
      "content" -> nonEmptyText
    )(SendEmail.apply)(SendEmail.unapply))

  val reminderApprovalForm = Form(
    mapping(
      "confirm" -> nonEmptyText
    )(identity[String])(Option.apply)
  )

  val reminderEmailSubject = "Final Ticket Balance Reminder"

  val reminderEmailText = StringContext("Hello ", ",<br /><br />" +
    "Just a quick reminder that the remaining balance for your ticket to Wadham Ball 2016 is due. Your remaining balance can be paid here:<br /><br />" +
    "<a href='http://wadhamball.co.uk/settle/", "'>http://wadhamball.co.uk/settle/","</a><br /><br />" +
    "We look forward to seeing you at Wadham Ball 2016.<br /><br />" +
    "If you have any questions, feel free to send an email to <a href='mailto:ball.president@wadh.ox.ac.uk'>ball.president@wadh.ox.ac.uk</a> for help.<br /><br />" +
    "Best regards,<br />" +
    "Wadham Ball Committee")

//  val sendForm = Form(
//    mapping(
//      "subject" -> nonEmptyText,
//      "content" -> nonEmptyText,
//      "email" -> email
//    )
//  ((subject: String, content: String, email: String) => {
//    val approvalId = RandomStringUtils.randomAlphanumeric(20)
//    new SendRequest(None, subject, content, email, approvalId, false)
//  })((x:SendRequest) => Option(x.subject, x.content, x.email)))

  // -- Actions

  def index = Action { Home }

  def home = Action.async { implicit rs =>
    Future { Ok(html.main(emailForm))}
  }

  def subscription = Action.async { implicit rs =>
    Future { Ok(html.subscription(emailForm)) }
  }

  def subscribe = Action.async { implicit rs =>
    emailForm.bindFromRequest.fold(
      formWithErrors => Future { BadRequest(html.subscription(formWithErrors)) },
      email => {
        try {
          emailsDAO.insert(email)
          Future { Home.flashing("success" -> "Email %s has been added".format(email.email)) }
        } catch {
          case ex: JdbcSQLException => Future { Home.flashing("success" -> "Email %s already present".format(email.email)) }
        }
      })
  }

  def unsubscribe(recipient: String) = Action { request =>
    Await.result(emailsDAO.remove(recipient), Duration.Inf)
    Ok("Successfully unsubscribed")
  }

  def sendmail = Action.async{ implicit rs =>
    Future { Ok(html.sendmail(emailSendForm)) }
  }

  def sendnews = Action.async { implicit rs =>
    emailSendForm.bindFromRequest.fold(
      formWithErrors => Future { BadRequest(html.sendmail(formWithErrors)) },
      emailRequest => {
        val response = mailer.sendMail(Await.result(emailsDAO.getAll, Duration.Inf).map(email => email.email), emailRequest.subject, emailRequest.content, true)
        if (!Await.result(response, Duration.Inf)) {
          Logger.error("Failed to send an email!")
          Future { EmailSend.flashing("failure" -> "Message failed to send") }
        }
        else Future { EmailSend.flashing("success" -> "Message sent") }
      }
    )
  }

  def getreminder = Action.async { implicit rs =>
    Future { Ok(html.sendreminder(reminderApprovalForm)) }
  }

  def sendreminder = Action.async { implicit rs =>
    reminderApprovalForm.bindFromRequest.fold(
      formWithErrors => Future { BadRequest(html.sendreminder(formWithErrors)) },
      confirmationString => {
        if (confirmationString.equalsIgnoreCase("confirm")) {
          // Admin has confirmed, lets send some emails
          val unpaidTickets = Await.result(ticketDAO.getUnpaid, Duration.Inf)
          val responses = for (ticket <- unpaidTickets) yield mailer.sendMail(Seq(ticket.email), reminderEmailSubject, reminderEmailText.s(ticket.firstName, ticket.id.get, ticket.id.get), false)
          val response = Await.result(Future.sequence(responses), Duration.Inf)
          val errors = response.zipWithIndex.collect { case (false, i) => s"""Error sending reminder for ticket ${unpaidTickets(i)}""" }
          if (errors.nonEmpty) {
            errors.foreach(err => Logger.error(err))
            Future { ReminderSend.flashing("failure" -> "Failed to send some reminders - see logs.") }
          }
          else Future { ReminderSend.flashing("success" -> "Message sent") }
        }
        else {
          // Not confirmed, redisplay form with flashing failure
          Future { ReminderSend.flashing("failure" -> "Admin did not confirm action") }
        }
      }
    )
  }


}
