package com.cqrs.cloud.query

import com.cqrs.cloud.util.DatabaseService

import scala.concurrent.{ ExecutionContext, Future }

class UserRepository(val databaseService: DatabaseService)(implicit executionContext: ExecutionContext)
		extends UserEntityTable {

	import databaseService._
	import databaseService.driver.api._

	def getUsers(): Future[Seq[UserEntity]] = db.run(users.result)

	def getUserById(id: Long): Future[Option[UserEntity]] = db.run(users.filter(_.id === id).result.headOption)

	def getUserByEmail(email: String): Future[Option[UserEntity]] =
		db.run(users.filter(_.email === email).result.headOption)

	def createUser(user: UserEntity): Future[Long] = db.run((users returning users.map(_.id)) += user)

	def deleteUser(id: Long): Future[Int] = db.run(users.filter(_.id === id).delete)

	def createTable(): Future[Unit] = {
		db.run(DBIO.seq(users.schema.create))
	}
}
