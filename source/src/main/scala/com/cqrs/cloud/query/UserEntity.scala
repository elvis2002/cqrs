package com.cqrs.cloud.query

import java.sql.Timestamp

import com.cqrs.cloud.domain.User
import com.cqrs.cloud.util.DatabaseService
import slick.profile.SqlProfile
import slick.profile.SqlProfile.ColumnOption.SqlType

final case class UserEntity(
	id: Option[Long] = None,
	createdAt: Option[Timestamp] = None,
	updatedAt: Option[Timestamp] = None,
	messageSeqNr: Long,
	userInfo: User)

trait UserEntityTable {

	protected val databaseService: DatabaseService
	import databaseService.driver.api._

	class Users(tag: Tag) extends Table[UserEntity](tag, "CLOUD_USERS") {
		def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
		def createdAt =
			column[Timestamp]("CREATED_AT", SqlType("timestamp not null default CURRENT_TIMESTAMP"))
		def updatedAt =
			column[Timestamp](
				"UPDATED_AT",
				SqlType("timestamp not null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP"))
		def messageSeqNr = column[Long]("MSG_SEQ_NR")
		def email = column[String]("EMAIL")
		def firstName = column[String]("LAST_NAME")
		def lastName = column[String]("FIRST_NAME")

		def * =
			(id.?, createdAt.?, updatedAt.?, messageSeqNr, (email, firstName, lastName)).shaped <> ({
				case (id, createdAt, updatedAt, messageSeqNr, userInfo) =>
					UserEntity(id, createdAt, updatedAt, messageSeqNr, User.tupled.apply(userInfo))
			}, { ue: UserEntity =>
				def f1(u: User) = User.unapply(u).get
				Some((ue.id, ue.createdAt, ue.updatedAt, ue.messageSeqNr, f1(ue.userInfo)))
			})

		def idx_user = index("idx_user", email, unique = true)
	}

	protected val users = TableQuery[Users]

}
