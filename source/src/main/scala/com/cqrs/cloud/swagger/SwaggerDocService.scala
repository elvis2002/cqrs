package com.cqrs.cloud.swagger

import akka.actor._

import com.github.swagger.akka.SwaggerHttpService
import com.github.swagger.akka.model.Info
import com.cqrs.cloud.UserService
import io.swagger.models.ExternalDocs
import io.swagger.models.auth.BasicAuthDefinition
import scala.reflect.runtime.{ universe => ru }

class SwaggerDocService(ip: String, port: Int) extends SwaggerHttpService {
	override val apiClasses: Set[Class[_]] = Set(classOf[UserService])
	override val host = ip + ":" + port
	override val info = Info(version = "1.0")
	override val basePath = "/"
}