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

@Singleton()
class Mailer @Inject()(implicit executionContext: ExecutionContext) {
  val config = ConfigFactory.parseFile(new File("conf/mailer.conf")).resolve()
  val server = config.getString("server")
  val apiKey = config.getString("apiKey")
  val timeout = config.getLong("timeout")
  val fromName = config.getString("fromName")
  val fromEmail = config.getString("fromEmail")

  val UNSUB_STRING =
    """
      |
      |<br>
      |<p>To unsubscribe click: <a href="%tag_unsubscribe_url%">%tag_unsubscribe_url%</a></p>
      |
    """.stripMargin

  def sendMail(targets: Seq[String], subject: String, content: String): Future[Boolean] = {
    Future.reduce(targets
      .grouped(900)
      .map((emails:Seq[String]) => doSend(emails, subject, content))
    )(_ && _)
  }

  def doSend(emails: Seq[String], subject: String, content: String): Future[Boolean] = {
    WS.url("https://api.mailgun.net/v3/" + server + "/messages")
      .withAuth("api", apiKey, WSAuthScheme.BASIC)
      .withRequestTimeout(timeout)
      .post(Map(
      "subject" -> Seq(subject),
      "from" -> Seq(fromName + " <" + fromEmail + ">"),
      "to" -> emails,
      "html" -> Seq(content + UNSUB_STRING),
      "o:tag" -> Seq("newsletter")
    ))
    .map(response => response.status == 200)
  }
}