package com.cqrs.cloud

import akka.actor.{ Actor, ExtendedActorSystem, Extension, ExtensionKey }

import scala.concurrent.duration.{ FiniteDuration, MILLISECONDS }

object Settings extends ExtensionKey[Settings]

class Settings(system: ExtendedActorSystem) extends Extension {

	object httpService {
		val address: String = cloudApp.getString("http-service.address")
		val port: Int = cloudApp.getInt("http-service.port")
		val selfTimeout: FiniteDuration = getDuration("http-service.self-timeout")
	}

	object rabbitMQ {
		val uri: String = cloudApp.getString("rabbitmq.uri")
	}

	object mariaDB {
		val uri: String = cloudApp.getString("mariadb.uri")
		val user: String = cloudApp.getString("mariadb.user")
		val password: String = cloudApp.getString("mariadb.password")
	}

	private val cloudApp = system.settings.config.getConfig("cloud-app")

	private def getDuration(key: String) = FiniteDuration(cloudApp.getDuration(key, MILLISECONDS), MILLISECONDS)
}

trait ActorSettings {
	this: Actor =>
	val settings: Settings = Settings(context.system)
}
