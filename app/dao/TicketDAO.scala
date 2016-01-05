package dao

import javax.inject.{Inject, Singleton}

import models.Ticket
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

trait TicketComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import driver.api._

  class Tickets(tag: Tag) extends Table[Ticket](tag, "TICKET") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def firstName = column[String]("FIRSTNAME")
    def lastName = column[String]("LASTNAME")
    def email = column[String]("EMAIL")
    def depositTransaction = column[Option[String]]("DEPOSITTRANSACTION")
    def finalTransaction = column[Option[String]]("FINALTRANSACTION")
    def isDining = column[Option[Boolean]]("ISDINING")
    def * = (id.?, firstName, lastName, email, depositTransaction, finalTransaction, isDining) <> (Ticket.tupled, Ticket.unapply)
  }
}

@Singleton()
class TicketDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends TicketComponent
with HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  val maxDining = 139

  val tickets = TableQuery[Tickets]

  def contains(email:String): Future[Boolean] =
    db.run(tickets.filter(_.email === email).exists.result)

  def getAll: Future[Seq[Ticket]] =
    db.run(tickets.result)

  def getUnpaid: Future[Seq[Ticket]] =
    db.run(tickets.filter(_.finalTransaction.isEmpty).result)

  /** Insert a new ticket */
  def insert(ticket: Ticket): Future[Long] =
    db.run(tickets returning tickets.map(_.id) += ticket)

  def insert(tickets: Seq[Ticket]): Future[Unit] =
    db.run(this.tickets ++= tickets).map(_ => ())

  def diningAvailable: Future[Boolean] =
    db.run((tickets.filter(_.isDining).length < 1).result)

  def getName(id: Long): Future[Option[String]] =
    db.run(tickets.filter(_.id === id).map(_.firstName).result.headOption)

  def getEmail(id: Long): Future[Option[String]] =
    db.run(tickets.filter(_.id === id).map(_.email).result.headOption)

  def hasPaid(id: Long): Future[Boolean] =
    db.run(tickets.filter(_.id === id).filterNot(_.finalTransaction.isEmpty).exists.result)

  def count: Future[Int] =
    db.run(tickets.length.result)

  def addPayment(id: Long, transaction: String): Future[Unit] =
    db.run((for { t <- tickets if t.id === id } yield t.finalTransaction).update(Some(transaction))).map(_ => ())

  def makeDining(id: Long): Future[Unit] =
    db.run((for { t <- tickets if t.id === id } yield t.isDining).update(Some(true)).map(_ => ()))
}