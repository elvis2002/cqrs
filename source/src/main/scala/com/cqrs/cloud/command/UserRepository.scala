package com.cqrs.cloud.command

import akka.actor.{ ActorLogging, Props }
import akka.persistence.PersistentActor
import com.cqrs.cloud.domain.User
import com.cqrs.cloud.command.UserRepository.{ AddUser, ConfirmAddUser, GetUsers }

object UserRepository {

	final val Name = "user-repository"

	def props(): Props = Props(new UserRepository())

	case object GetUsers
	final case class AddUser(deliveryId: Long, user: User)
	final case class ConfirmAddUser(deliveryId: Long)
}

class UserRepository extends PersistentActor with ActorLogging {

	override val persistenceId: String = "user-repository"
	private var users = Set.empty[User]

	override def receiveCommand: Receive = {
		case GetUsers =>
			sender() ! users
		case AddUser(id, user) =>
			log.info(s"Adding $id new user with email; ${user.email}")
			persist(user) { persistedUser =>
				receiveRecover(persistedUser)
				sender() ! ConfirmAddUser(id)
			}
	}

	override def receiveRecover: Receive = {
		case user: User => users += user
	}
}
