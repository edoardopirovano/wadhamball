package controllers

import java.sql.Timestamp
import javax.inject.Inject
import dao.EmailsDAO
import models.Email
import play.api.data.Form
import play.api.data.Forms.{mapping, email}
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, Controller}
import views.html

import scala.concurrent.Future

/** Manage a database of computers. */
class Application @Inject() (emailsDAO: EmailsDAO, val messagesApi: MessagesApi) extends Controller with I18nSupport {

  val Home = Redirect(routes.Application.home())

  val emailForm = Form(
    mapping(
      "email" -> email
    )
      ((x:String) => new Email(x, new Timestamp(System.currentTimeMillis())))
      ((x:Email) => Option(x.email)))

  // -- Actions

  def index = Action { Home }

  def home = Action.async { implicit rs =>
    Future { Ok(html.home(emailForm)) }
  }

  def subscribe = Action.async { implicit rs =>
    emailForm.bindFromRequest.fold(
      formWithErrors => Future { BadRequest(html.home(formWithErrors)) },
      email => {
        for {
          _ <- emailsDAO.insert(email)
        } yield Home.flashing("success" -> "Email %s has been added".format(email.email))
      })
  }

  def unsubscribe(email: String) = Action.async { implicit rs =>
     for {
          _ <- emailsDAO.remove(email)
     } yield Home.flashing("success" -> "Email %s has been unsubscribed".format(email))
  }
}
