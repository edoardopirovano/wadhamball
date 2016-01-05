package models

import java.sql.Timestamp

case class Email(email: String, joinDate: Timestamp)
//case class SendRequest(id: Option[Long], subject: String, content: String, email: String, approvalId: String, sent: Boolean)

case class SendEmail(subject: String, content: String)

case class Ticket(id: Option[Long], firstName: String, lastName: String, email: String, depositTransaction: Option[String], finalTransaction: Option[String], isDining: Option[Boolean])

case class Registration(firstName: String, lastName: String, email:String, isWadham: Boolean, numberOfGuests: Int)

case class DepositForm(firstName: String, lastName: String, email:String, payment_method_nonce: String)

case class SettleForm(id: Long, diningUpgrade: Boolean,payment_method_nonce: String )