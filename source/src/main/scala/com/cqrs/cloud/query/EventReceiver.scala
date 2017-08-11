package com.cqrs.cloud.query

import akka.actor.Status.Failure
import akka.actor.{ ActorLogging, Props }
import akka.camel.{ Ack, CamelMessage, Consumer }
import cats.implicits._
import com.cqrs.cloud.ActorSettings
import com.cqrs.cloud.domain.User
import org.apache.camel.component.rabbitmq.RabbitMQConstants

object EventReceiver {
	final val Name = "event-receiver"

	def props(userRepository: UserRepository): Props = Props(new EventReceiver(userRepository))
}

class EventReceiver(userRepository: UserRepository) extends Consumer with ActorSettings with ActorLogging {

	import io.circe._
	import io.circe.generic.auto._
	import io.circe.parser._
	import context.dispatcher

	override def endpointUri: String = settings.rabbitMQ.uri

	override def autoAck = false

	override def receive: Receive = {
		case msg: CamelMessage =>
			val origSender = sender()
			val body: Either[Error, User] = decode[User](msg.bodyAs[String])

			body.fold({ error =>
				log.error("Could not parse message: {}", msg)
				origSender ! Failure(error)
			}, { user =>
				val messageId: Long = msg.headers.get(RabbitMQConstants.MESSAGE_ID) match {
					case Some(id: Long) => id
					case Some(id: String) => id.toLong
					case _ => -1
				}
				log.info("Event Received with id {} and for user: {}", messageId, user.email)

				userRepository.getUserByEmail(user.email).foreach {
					case Some(_) => log.debug("User with email {} already exists",user.email)
					case None =>
						userRepository.createUser(UserEntity(messageSeqNr = messageId, userInfo = user)).onComplete {
							case scala.util.Success(_) => origSender ! Ack
							case scala.util.Failure(t) => log.error(t, "Failed to persist user with email: {}", user.email)
						}
				}
			})
		case _ => log.warning("Unexpected event received")
	}

}
