package dao

import javax.inject.{Inject, Singleton}

import models.Ticket
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile

import scala.concurrent.Future

trait TicketComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import driver.api._

  class Tickets(tag: Tag) extends Table[Ticket](tag, "TICKET") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def firstName = column[String]("FIRSTNAME")
    def lastName = column[String]("LASTNAME")
    def email = column[String]("EMAIL")
    def depositTransaction = column[Option[String]]("DEPOSITTRANSACTION")
    def finalTransaction = column[Option[String]]("FINALTRANSACTION")
    def * = (id.?, firstName, lastName, email, depositTransaction, finalTransaction) <> (Ticket.tupled, Ticket.unapply)
  }
}

@Singleton()
class TicketDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends TicketComponent
with HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  val tickets = TableQuery[Tickets]

  def contains(email:String): Future[Boolean] =
    db.run(tickets.filter(_.email === email).exists.result)

  def getAll: Future[Seq[Ticket]] =
    db.run(tickets.result)

  /** Insert a new ticket */
  def insert(ticket: Ticket): Future[Long] =
    db.run(tickets returning tickets.map(_.id) += ticket)
}