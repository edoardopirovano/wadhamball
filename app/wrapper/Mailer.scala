package wrapper;

import com.google.common.net.MediaType
import com.typesafe.config.ConfigFactory;
import play.Configuration
;
import play.api.libs.json._;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File

import play.api.libs.ws.{WSAuthScheme, WS}
import play.twirl.api.Html

import scala.concurrent.{Future, ExecutionContext}

import play.api.Play.current

import play.api.Play.current

@Singleton()
class Mailer @Inject()(implicit executionContext: ExecutionContext) {
  val server = current.configuration.getString("mailgun.server").get
  val apiKey = current.configuration.getString("mailgun.apiKey").get
  val timeout = current.configuration.getLong("mailgun.timeout").get
  val fromName = current.configuration.getString("mailgun.fromName").get
  val fromEmail = current.configuration.getString("mailgun.fromEmail").get

  val UNSUB_STRING =
    """
      |
      |<br>
      |<p>To unsubscribe click: <a href="%tag_unsubscribe_url%">%tag_unsubscribe_url%</a></p>
      |
    """.stripMargin

  def sendMail(targets: Seq[String], subject: String, content: String, unsub: Boolean): Future[Boolean] = {
    Future.reduce(targets
      .grouped(900)
      .map((emails:Seq[String]) => doSend(emails, subject, content, unsub))
    )(_ && _)
  }

  private def doSend(emails: Seq[String], subject: String, content: String, unsub: Boolean): Future[Boolean] = {
    WS.url("https://api.mailgun.net/v3/" + server + "/messages")
      .withAuth("api", apiKey, WSAuthScheme.BASIC)
      .withRequestTimeout(timeout)
      .post(Map(
      "subject" -> Seq(subject),
      "from" -> Seq(fromName + " <" + fromEmail + ">"),
      "to" -> emails,
      "recipient-variables" -> Seq("{" + emails.map((email: String) => "\"" + email + "\": {}").mkString(",") + "}"),
      "html" -> Seq(content + (if (unsub) UNSUB_STRING else "")),
      "o:tag" -> (if (unsub) Seq("newsletter") else Seq())
    ))
    .map(_.status == 200)
  }
}