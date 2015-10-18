package models

import java.sql.Timestamp

case class Email(email: String, joinDate: Timestamp)
//case class SendRequest(id: Option[Long], subject: String, content: String, email: String, approvalId: String, sent: Boolean)