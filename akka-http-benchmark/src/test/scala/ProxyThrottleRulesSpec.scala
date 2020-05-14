import org.scalatest.{ Matchers, WordSpec }
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import Directives._
import akka.http.scaladsl.model.headers.HttpEncodings
import akka.http.scaladsl.model.headers.HttpEncoding
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.MediaTypes
import akka.testkit.TestKit

class ProxyThrottleRulesSpec extends WordSpec with Matchers with ScalatestRouteTest {

  val subjectRoute = ProxyConfig.configRoutes
  val configByIp = """{
        "fieldRestriction": "IP",
        "valueMatch": "127.0.0.10",
        "time": "5 seconds",
        "max": 2
      }"""
  val changeConfigByIp = """{
        "fieldRestriction": "IP",
        "valueMatch": "127.0.0.10",
        "time": "5 seconds",
        "max": 1
      }"""
  val newConfigIp = """{
        "fieldRestriction": "IP",
        "valueMatch": "127.0.0.9",
        "time": "5 seconds",
        "max": 1
      }"""

  override def afterAll: Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }
  
  "The Configuration Proxy " should {
    "return a MethodNotAllowed error for PUT requests" in {
      // tests:
      Put("/throttle/config") ~> Route.seal(subjectRoute) ~> check {
        status shouldEqual StatusCodes.MethodNotAllowed
      }
    }

    "create the internal value for POST with the json rule" in {
      // tests:
      Post("/throttle/config", HttpEntity(MediaTypes.`application/json`, configByIp)) ~> subjectRoute ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "returns the created rule" in {
      // tests:
      Get("/throttle/config") ~> subjectRoute ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] should include(""""IP":{"127.0.0.10":{"window":"5 seconds","maxAllowed":"2"}}""")
      }
    }

    "update the 'maxAllow' for a POST with the same type and value and diferent 'maxAllowed'" in {
      // tests:
      Post("/throttle/config", HttpEntity(MediaTypes.`application/json`, changeConfigByIp)) ~> subjectRoute ~> check {
        status shouldEqual StatusCodes.OK
      }
      Get("/throttle/config") ~> subjectRoute ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] should include(""""IP":{"127.0.0.10":{"window":"5 seconds","maxAllowed":"1"}}""")
      }
    }

    "append the ip value for a POST with the SAME type and DIFERENT ip value" in {
      // tests:
      Post("/throttle/config", HttpEntity(MediaTypes.`application/json`, newConfigIp)) ~> subjectRoute ~> check {
        status shouldEqual StatusCodes.OK
      }
      Get("/throttle/config") ~> subjectRoute ~> check {
        status shouldEqual StatusCodes.OK
        println(responseAs[String])
        responseAs[String] should include(""""IP":{"127.0.0.10":{"window":"5 seconds","maxAllowed":"1"},"127.0.0.9":{"window":"5 seconds","maxAllowed":"1"}}""")
      }
    }
    
    "saves any type of fieldRestriction if wont match with IP or PATH" in {
      val invalid = """{
        "fieldRestriction": "AAAAAAA",
        "valueMatch": "127.0.0.9",
        "time": "5 seconds",
        "max": 1
      }"""
      Post("/throttle/config", HttpEntity(MediaTypes.`application/json`, invalid)) ~> Route.seal(subjectRoute) ~> check {
        status shouldEqual StatusCodes.OK
      }
      Get("/throttle/config") ~> subjectRoute ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] should include(""""IP":{"127.0.0.10":{"window":"5 seconds","maxAllowed":"1"},"127.0.0.9":{"window":"5 seconds","maxAllowed":"1"}},"AAAAAAA":{"127.0.0.9":{"window":"5 seconds","maxAllowed":"1"}}""")
      }
    }
  }
}