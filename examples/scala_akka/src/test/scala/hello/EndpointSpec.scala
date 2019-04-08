package hello
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}

class EndpointSpec extends WordSpec with Matchers with ScalatestRouteTest {
  Endpoint.getClass.getSimpleName should {
    "should return status OK" in {
      Get() ~> Endpoint.route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
  }
}
