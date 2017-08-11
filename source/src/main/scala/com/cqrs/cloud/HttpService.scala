package com.cqrs.cloud

import javax.ws.rs.Path

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives
import akka.stream.{ ActorMaterializer, Materializer }
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import de.heikoseeberger.akkahttpcirce.CirceSupport
import com.cqrs.cloud.swagger.SwaggerDocService
import com.cqrs.cloud.util.CorsSupport

import scala.concurrent.ExecutionContext
import io.swagger.annotations._
import com.cqrs.cloud.domain._
import com.cqrs.cloud.query.UserRepository

import scala.util.{ Failure, Success }

object HttpService extends CorsSupport {

	private[cloud] case object Stop

	final val Name = "http-service"

	def props(
		address: String,
		port: Int,
		internalTimeout: Timeout,
		userAggregate: ActorRef,
		userRepository: UserRepository): Props =
		Props(new HttpService(address, port, internalTimeout, userAggregate, userRepository))

	private[cloud] def route(
		httpService: ActorRef,
		userService: UserService,
		swaggerDocService: SwaggerDocService)(implicit ec: ExecutionContext, mat: Materializer) = {
		import Directives._
		import io.circe.generic.auto._

		def assets = pathPrefix("swagger") {
			getFromResourceDirectory("swagger") ~ pathSingleSlash(get(redirect("index.html", StatusCodes.PermanentRedirect)))
		}

		def start = pathSingleSlash {
			redirect("/swagger/index.html", StatusCodes.PermanentRedirect)
		}
		
		assets ~ start ~ corsHandler(userService.route) ~ corsHandler(swaggerDocService.routes)
	}
}

class HttpService(address: String, port: Int, internalTimeout: Timeout, userAggregate: ActorRef, userRepository: UserRepository)
		extends Actor with ActorLogging {
	import HttpService._
	import context.dispatcher

	private implicit val mat = ActorMaterializer()

	Http(context.system)
		.bindAndHandle(
			route(self, new UserService(userAggregate, userRepository, internalTimeout), new SwaggerDocService(address, port)),
			address,
			port)
		.pipeTo(self)

	override def receive = binding

	private def binding: Receive = {
		case serverBinding @ Http.ServerBinding(address) =>
			log.info("Listening on {}", address)
			context.become(bound(serverBinding))

		case Status.Failure(cause) =>
			log.error(cause, s"Can't bind to $address:$port")
			context.stop(self)
	}

	private def bound(serverBinding: Http.ServerBinding): Receive = {
		case Stop =>
			serverBinding.unbind()
			context.stop(self)
	}
}

@Path("/users") // @Path annotation required for Swagger
@Api(value = "/users", produces = "application/json")
class UserService(userAggregate: ActorRef, userRepository: UserRepository, internalTimeout: Timeout)(implicit executionContext: ExecutionContext) extends Directives {
	import CirceSupport._
	import io.circe.generic.auto._

	implicit val timeout = internalTimeout

	val route = pathPrefix("users") { usersGetAll ~ userPost }

	@ApiOperation(value = "Get list of all users", nickname = "getAllUsers", httpMethod = "GET",
		response = classOf[User], responseContainer = "Set")
	@ApiResponses(Array(
		new ApiResponse(code = 200, message = "System fetch success"),
		new ApiResponse(code = 500, message = "Internal error")))
	def usersGetAll = get {
		onComplete(userRepository.getUsers()) {
			case Success(users) => complete(users.map(ue => ue.userInfo))
			case Failure(t) => complete(HttpResponse(StatusCodes.InternalServerError, entity = "Internal error: " + t))
		}
	}

	@ApiOperation(value = "Create new user", nickname = "userPost", httpMethod = "POST", produces = "text/plain")
	@ApiImplicitParams(Array(
		new ApiImplicitParam(name = "user", dataType = "com.cqrs.cloud.domain.User", paramType = "body", required = true)))
	@ApiResponses(Array(
		new ApiResponse(code = 201, message = "User created"),
		new ApiResponse(code = 409, message = "User already exists")))
	def userPost = post {
		entity(as[User]) { user =>
			println(user)
			onSuccess(userAggregate ? AddUserCmd(user)) {
				case UserAddedResp(_) => complete(HttpResponse(StatusCodes.Created, entity = "User added"))
				case UserExistsResp(_) => complete(HttpResponse(StatusCodes.Conflict, entity = "User already exists"))
			}
		}
	}
}
