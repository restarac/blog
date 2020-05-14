import org.scalatest.{ Matchers, WordSpec }
import org.scalatest.matchers._
import org.scalatest.matchers.should._
import org.scalatest.matchers.dsl._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import Directives._
import akka.http.scaladsl.model.headers.HttpEncodings
import akka.http.scaladsl.model.headers.HttpEncoding
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.MediaTypes
import akka.testkit.TestKit
import akka.stream.scaladsl.Flow
import akka.http.scaladsl.model.HttpRequest

class ForwardRequestSpec extends WordSpec with Matchers with ScalatestRouteTest {
  import scala.concurrent.duration._
  import akka.http.scaladsl.testkit.RouteTestTimeout
  import akka.testkit.TestDuration

  implicit val timeout = RouteTestTimeout(7.seconds.dilated)

  val subjectRoute = ProxyRequest.dynamicRoutes
  val configByPath = """{
        "fieldRestriction": "PATH",
        "valueMatch": "/sites/MLB/categories",
        "time": "1 seconds",
        "max": 1
      }"""

  override def afterAll: Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

  "The ProxyRequest " can {
    
    "forward" should {
      "returns a response from api.mercadolibre" in {
        // tests:
        Get("/sites/MLB/categories") ~> subjectRoute ~> check {
          status shouldEqual StatusCodes.OK
          responseAs[String] should include("Acessórios para Veículos")
        }
      }
    }

    "throttle" should {
      // configs the proxy
      Post("/throttle/config", HttpEntity(MediaTypes.`application/json`, configByPath)) ~> ProxyConfig.configRoutes() ~> check {
        status shouldEqual StatusCodes.OK
      }
      
      "returns too many requests " in {
        // make first request
        Get("/sites/MLB/categories") ~> subjectRoute ~> check {
          status shouldEqual StatusCodes.OK
          responseAs[String] should include("Acessórios para Veículos")
        }
        // make first request
        Get("/sites/MLB/categories") ~> subjectRoute ~> check {
          status shouldEqual StatusCodes.TooManyRequests
        }
      }

      "returns the request after the throttle time" in {
        // wait the timeout for the throttle rule
        Thread.sleep(2000)
        Get("/sites/MLB/categories") ~> subjectRoute ~> check {
          status shouldEqual StatusCodes.OK
        }
      }
    }
  }
}