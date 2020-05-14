import scala.concurrent.Future
import scala.io.StdIn

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import spray.json.JsValue
import spray.json.DefaultJsonProtocol._

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.coding.Deflate
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.StatusCodes.MovedPermanently
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import spray.json.DefaultJsonProtocol
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server._
import Directives._
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.ContentTypes
import spray.json._
//import kamon.Kamon

import akka.http.scaladsl.model.StatusCodes
import akka.actor.{ Actor, ActorRef, ActorSystem, PoisonPill, Props }

object ProxyServer extends App {
  implicit val system = ActorSystem("Proxy")
  implicit val executionContext = system.dispatcher

  val port = 1080
  val host = "127.0.0.1"

  //Actual binding on the host ports
  val bindingFuture2 = Http().bindAndHandle(ProxyRequest.dynamicRoutes, host, port)

  system.log.info("Server online at http://" + host + "/" + port + "/")
}
