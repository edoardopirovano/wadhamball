package dao

import java.sql.Timestamp
import javax.inject.{Inject, Singleton}

import models.Email
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

trait EmailComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import driver.api._

  class Emails(tag: Tag) extends Table[Email](tag, "EMAIL") {
    def email = column[String]("EMAIL", O.PrimaryKey)
    def joinDate = column[Timestamp]("JOINDATE")
    def * = (email, joinDate) <> (Email.tupled, Email.unapply)
  }
}

@Singleton()
class EmailsDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends EmailComponent
  with HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  val emails = TableQuery[Emails]

  /** Get all emails **/
  def getAll: Future[Seq[Email]] =
    db.run(emails.result)

  /** Insert a new email */
  def insert(email: Email): Future[Unit] =
    db.run(emails += email).map(_ => ())

  def insert(emails: Seq[Email]): Future[Unit] =
    db.run(this.emails ++= emails).map(_ => ())

  /** Remove an email */
  def remove(email: String): Future[Unit] =
    db.run(emails.filter(_.email === email).delete).map(_ => ())

  /** Count emails present */
  def count: Future[Int] =
    db.run(emails.length.result)
}