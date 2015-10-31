package controllers

import java.sql.Timestamp
import javax.inject.Inject

import models.{Registration, Email}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller}
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import views.html

import scala.concurrent.Future

/**
 * Created by Richard on 31/10/2015.
 */
class RegistrationController @Inject() (val messagesApi: MessagesApi) extends Controller with I18nSupport {

  val registrationForm = Form(
    mapping(
      "firstName" -> nonEmptyText(1, 15),
      "lastName" -> nonEmptyText(1, 15),
      "email" -> email,
      "isWadham" -> checked("Are you a Wadham student?"),
      "numberOfGuests" -> number(0, 3)
    )(Registration.apply)(Registration.unapply))

  val registration = Action.async { implicit rs =>
    Future { Ok(html.registration(registrationForm)) }
  }

  val register = Action.async { implicit rs =>
    Future { Ok(html.registration(registrationForm)) }
  }

}
