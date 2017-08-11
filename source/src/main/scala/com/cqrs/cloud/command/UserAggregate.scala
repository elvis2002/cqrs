package com.cqrs.cloud.command

import akka.actor.{ ActorLogging, ActorRef, Props, SupervisorStrategy }
import akka.persistence.{ AtLeastOnceDelivery, PersistentActor }
import com.cqrs.cloud.domain._
import com.cqrs.cloud.command.EventSender.{ Confirm, Msg }
import com.cqrs.cloud.command.UserAggregate.{ Evt, GetUsersForwardResponse, MsgAddUser, MsgConfirmed }
import com.cqrs.cloud.command.UserRepository.{ AddUser, ConfirmAddUser, GetUsers }

object UserAggregate {

	final val Name = "user-aggregate"

	def props(): Props = Props(new UserAggregate())

	sealed trait Evt

	final case class MsgAddUser(u: User) extends Evt

	final case class MsgConfirmed(deliveryId: Long) extends Evt

	final case class GetUsersForwardResponse(senderActor: ActorRef, existingUsers: Set[User], newUser: User)
}

class UserAggregate extends PersistentActor with AtLeastOnceDelivery with ActorLogging {

	import akka.pattern.{ ask, pipe }
	import akka.util.Timeout
	import context.dispatcher

	import scala.concurrent.duration._

	override val persistenceId: String = "user-aggregate"
	override val supervisorStrategy = SupervisorStrategy.stoppingStrategy
	implicit val timeout = Timeout(100 milliseconds)

	private val userRepository = context.watch(createUserRepository())
	private val eventSender = context.watch(createEventSender())

	protected def createUserRepository(): ActorRef = {
		context.actorOf(UserRepository.props(), UserRepository.Name)
	}

	protected def createEventSender(): ActorRef = {
		context.actorOf(EventSender.props(), EventSender.Name)
	}

	override def receiveCommand: Receive = {
		case AddUserCmd(newUser) =>
			val origSender = sender()
			val usersFuture = userRepository ? GetUsers
			pipe(usersFuture.mapTo[Set[User]].map(GetUsersForwardResponse(origSender, _, newUser))) to self

		case GetUsersForwardResponse(origSender, users, newUser) =>
			if (users.exists(_.email == newUser.email)) {
				origSender ! UserExistsResp(newUser)
			} else {
				persist(MsgAddUser(newUser)) { persistedMsg =>
					updateState(persistedMsg)
					origSender ! UserAddedResp(newUser)
				}
			}
		case ConfirmAddUser(deliveryId) =>
			persist(MsgConfirmed(deliveryId))(updateState)
		case Confirm(deliveryId) =>
			persist(MsgConfirmed(deliveryId))(updateState)
	}

	override def receiveRecover: Receive = {
		case evt: Evt => updateState(evt)
	}

	def updateState(evt: Evt): Unit = {
		evt match {
			case MsgAddUser(u) =>
				deliver(eventSender.path)(deliveryId => Msg(deliveryId, u))
				deliver(userRepository.path)(deliveryId => AddUser(deliveryId, u))
			case MsgConfirmed(deliveryId) =>
				confirmDelivery(deliveryId)
		}
	}
}
