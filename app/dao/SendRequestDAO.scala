//package dao
//
//import java.sql.Timestamp
//import javax.inject.{Inject, Singleton}
//
//import models.{SendRequest, Email}
//import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
//import slick.driver.JdbcProfile
//import play.api.libs.concurrent.Execution.Implicits.defaultContext
//
//import scala.concurrent.Future
//
//trait SendRequestComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
//  import driver.api._
//
//  class Emails(tag: Tag) extends Table[SendRequest](tag, "EMAIL") {
//    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
//    def subject = column[String]("SUBJECT")
//    def content = column[String]("CONTENT")
//    def email = column[String]("EMAIL")
//    def approvalid = column[String]("APPROVALID")
//    def sent = column[Boolean]("SENT")
//    def * = (id.?, subject, content, email, approvalid, sent) <> (SendRequest.tupled, SendRequest.unapply)
//  }
//}
//
//@Singleton()
//class SendRequestDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends SendRequestComponent
//with HasDatabaseConfigProvider[JdbcProfile] {
//
//  import driver.api._
//
//  val requests = TableQuery[SendRequest]
//
//  /** Insert a new email */
//  def insert(sendRequest: SendRequest): Future[Unit] =
//    db.run(requests += sendRequest).map(_ => ())
//
//  def insert(emails: Seq[Email]): Future[Unit] =
//    db.run(this.requests ++= emails).map(_ => ())
//
//  /** Remove an email */
//  def sent(id: Long): Future[Unit] =
//    db.run(requests.filter(_.id === id))
//
//  /** Count emails present */
//  def count: Future[Int] =
//    db.run(requests.length.result)
//}