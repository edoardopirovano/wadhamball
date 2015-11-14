package dao

import java.sql.Timestamp
import javax.inject.{Inject, Singleton}

import models.{Ticket, Email}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

trait TicketComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import driver.api._

  class Tickets(tag: Tag) extends Table[Ticket](tag, "TICKET") {
    def firstName = column[String]("FIRSTNAME")
    def lastName = column[String]("LASTNAME")
    def email = column[String]("EMAIL")
    def depositOnly = column[Boolean]("DEPOSITONLY")
    def payBy = column[Timestamp]("PAYBY")
    def * = (firstName, lastName, email, depositOnly, payBy) <> (Ticket.tupled, Ticket.unapply)
  }
}

@Singleton()
class TicketDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends TicketComponent
with HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  val tickets = TableQuery[Tickets]

  /** Insert a new ticket */
  def insert(ticket: Ticket): Future[Unit] =
    db.run(tickets += ticket).map(_ => ())
}