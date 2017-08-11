
package com.cqrs.cloud

import akka.actor._
import com.cqrs.cloud.query.{ EventReceiver, UserRepository }
import com.cqrs.cloud.util.DatabaseService
import com.cqrs.cloud.command.UserAggregate

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object CloudApp {
	def main(args: Array[String]): Unit = {
		implicit val system = ActorSystem("cloudnative")

		system.actorOf(Props(new Cloud), "cloud-app-master")

		Await.ready(system.whenTerminated, Duration.Inf)
	}
}

class Cloud extends Actor with ActorLogging with ActorSettings {
	override val supervisorStrategy = SupervisorStrategy.stoppingStrategy

	//create datasource
	private val databaseService = createDatabaseService()
	//user repository
	private val userRepository = createUserRepository(databaseService)
	//create event receiver
	context.watch(createEventReceiver(userRepository))
	//create user aggregate
	private val userAggregate = context.watch(createUserAggregate())
	//bind port
	context.watch(createHttpService(userAggregate, userRepository))

	log.info("Up and running")

	override def receive = {
		case Terminated(actor) => onTerminated(actor)
	}

	protected def createUserAggregate(): ActorRef = {
		context.actorOf(UserAggregate.props(), UserAggregate.Name)
	}

	protected def createDatabaseService(): DatabaseService = {
		import settings.mariaDB._
		new DatabaseService(uri, user, password)
	}

	protected def createUserRepository(databaseService: DatabaseService): UserRepository = {
		import context.dispatcher
		val repository = new UserRepository(databaseService)
		repository.createTable()
		return repository
	}

	protected def createEventReceiver(userRepository: UserRepository): ActorRef = {
		context.actorOf(EventReceiver.props(userRepository), EventReceiver.Name)
	}

	protected def createHttpService(userAggregateActor: ActorRef, userRepository: UserRepository): ActorRef = {
		import settings.httpService._
		context
			.actorOf(HttpService.props(address, port, selfTimeout, userAggregateActor, userRepository), HttpService.Name)
	}

	protected def onTerminated(actor: ActorRef): Unit = {
		log.error("Terminating the system because {} terminated!", actor)
		context.system.terminate()
		databaseService.dbSession.close()
		databaseService.db.close()
	}
}
