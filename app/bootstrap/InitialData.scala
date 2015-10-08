package bootstrap

import java.sql.Timestamp
import java.text.SimpleDateFormat
import javax.inject.Inject
import dao.EmailsDAO
import models.Email
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Try


/** Initial set of data to be imported into the sample application. */
private[bootstrap] class InitialData @Inject() (emailsDAO: EmailsDAO) {

  def insert(): Unit = {
    import play.api.libs.concurrent.Execution.Implicits.defaultContext

    val insertInitialDataFuture = for {
      count <- emailsDAO.count if count == 0
      _ <- emailsDAO.insert(InitialData.emails)
    } yield ()

    Try(Await.result(insertInitialDataFuture, Duration.Inf))
  }

  insert()
}

private[bootstrap] object InitialData {
  private val sdf = new SimpleDateFormat("yyyy-MM-dd")

  def emails = Seq(
    Email("test@example.com", new Timestamp(sdf.parse("2015-05-05").getTime)),
    Email("test2@example.com", new Timestamp(sdf.parse("2014-04-04").getTime))
  )
}
