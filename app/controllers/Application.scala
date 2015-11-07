package controllers

import java.security.SecureRandom
import java.sql.Timestamp
import java.util.UUID
import javax.inject.Inject
import dao.EmailsDAO
import models.Email
import org.apache.commons.lang3.RandomStringUtils
import play.api.data.Form
import play.api.data.Forms.{mapping, email}
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, Controller}
import views.html
import wrapper.Mailer

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import views._
import models._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

/** Manage a database of computers. */
class Application @Inject() (emailsDAO: EmailsDAO, mailer: Mailer, val messagesApi: MessagesApi) extends Controller with I18nSupport {

  val Home = Redirect(routes.Application.home())
  val Subscribe = Redirect(routes.Application.subscription())
  val EmailSend = Redirect(routes.Application.sendmail())

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
    Future { Ok(html.main())}
  }

  def subscription = Action.async { implicit rs =>
    Future { Ok(html.subscription(emailForm)) }
  }

  def subscribe = Action.async { implicit rs =>
    emailForm.bindFromRequest.fold(
      formWithErrors => Future { BadRequest(html.subscription(formWithErrors)) },
      email => {
        for {
          _ <- emailsDAO.insert(email)
        } yield Subscribe.flashing("success" -> "Email %s has been added".format(email.email))
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
}
