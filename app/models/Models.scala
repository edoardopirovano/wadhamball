package models

import java.sql.Timestamp

case class Email(email: String, joinDate: Timestamp)
//case class SendRequest(id: Option[Long], subject: String, content: String, email: String, approvalId: String, sent: Boolean)

case class SendEmail(subject: String, content: String)

// TODO
case class Order()

// TODO
case class Ticket()

// TODO
case class Registration(firstName: String, lastName: String, email:String, isWadham: Boolean, numberOfGuests: Int)