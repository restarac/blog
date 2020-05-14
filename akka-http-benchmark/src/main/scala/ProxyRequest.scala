import scala.concurrent.Future
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RequestContext
import akka.http.scaladsl.server.RouteResult
import spray.json.DefaultJsonProtocol

class ProxyRequest(val req: HttpRequest)(implicit val system: ActorSystem) {
  import ProxyRequest._
  implicit val executionContext = system.dispatcher

  def forward(): Future[HttpResponse] = {
    val forwardUri = Uri.from(scheme = destinationScheme, host = destinationHost, path = req.uri.path.toString)
    val proxyReq = HttpRequest(method = req.method, uri = forwardUri, entity = req.entity)
    system.log.debug("The URI:" + proxyReq.uri.toString() + " ### The protocol:" + proxyReq.protocol)
    val responseFuture: Future[HttpResponse] = Http(system).singleRequest(proxyReq)
    responseFuture
  }
}

object ProxyRequest extends Directives with DefaultJsonProtocol {
  val destinationHost = "www.google.com.br"
  val destinationScheme = "https"

  def dynamicRoutes(implicit system: ActorSystem): RequestContext => Future[RouteResult] = {
      extractRequest { req =>
        onSuccess(new ProxyRequest(req).forward()) { resp =>
          complete(resp)
        }
      }
  }
}