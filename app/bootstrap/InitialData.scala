package bootstrap

import java.sql.Timestamp
import java.text.SimpleDateFormat
import javax.inject.Inject
import dao.{TicketDAO, EmailsDAO}
import models.{Ticket, Email}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Try


/** Initial set of data to be imported into the sample application. */
private[bootstrap] class InitialData @Inject() (emailsDAO: EmailsDAO, ticketsDAO: TicketDAO) {

  def insert(): Unit = {
    import play.api.libs.concurrent.Execution.Implicits.defaultContext

    val insertInitialDataFuture = for {
      count <- emailsDAO.count if count == 0
      _ <- emailsDAO.insert(InitialData.emails)
    } yield ()

    val insertInitialDataFutureTickets = for {
      count <- ticketsDAO.count if count == 0
      _ <- ticketsDAO.insert(InitialData.tickets)
    } yield ()

    Try(Await.result(insertInitialDataFuture, Duration.Inf))
    Try(Await.result(insertInitialDataFutureTickets, Duration.Inf))
  }

  insert()
}

private[bootstrap] object InitialData {
  private val sdf = new SimpleDateFormat("yyyy-MM-dd")

  def emails = Seq(
    Email("test@example.com", new Timestamp(sdf.parse("2015-05-05").getTime)),
    Email("test2@example.com", new Timestamp(sdf.parse("2014-04-04").getTime))
  )

  def tickets = Seq(
    Ticket(Some(1), "Edoardo", "Pirovano", "edododo_do@yahoo.com", Some("abcde"), None, Some(true), 0, None),
    Ticket(Some(2), "Edoardo", "Pirovano", "edododo_do@yahoo.com", Some("abcde"), Some("qwerty"), None, 0, None),
    Ticket(Some(3), "Richard", "Appleby", "edododo_do@yahoo.com", Some("abcde"), None, None, 0, None)
  )
}
