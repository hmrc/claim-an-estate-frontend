/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import models.EstatesStoreRequest
import org.scalatest.RecoverMethods
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.WireMockHelper

class EstatesStoreConnectorSpec extends AsyncWordSpec with Matchers with WireMockHelper with RecoverMethods {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  lazy val app = new GuiceApplicationBuilder()
    .configure(Seq("microservice.services.estates-store.port" -> server.port(), "auditing.enabled" -> false): _*)
    .build()

  lazy val connector: EstatesStoreConnector = app.injector.instanceOf[EstatesStoreConnector]

  lazy val url: String = "/estates-store/lock"

  val utr            = "1234567890"
  val internalId     = "some-authenticated-internal-id"
  val managedByAgent = true

  val request: EstatesStoreRequest = EstatesStoreRequest(internalId, utr, managedByAgent, estateLocked = false)

  private def wiremock(payload: String, expectedStatus: Int, expectedResponse: String) =
    server.stubFor(
      post(urlEqualTo(url))
        .withHeader(CONTENT_TYPE, containing("application/json"))
        .withRequestBody(equalTo(payload))
        .willReturn(
          aResponse()
            .withStatus(expectedStatus)
            .withBody(expectedResponse)
        )
    )

  "EstatesStoreConnector" must {

    "call POST /lock" which {

      "returns 201 CREATED" in {

        val json = Json.stringify(Json.toJson(request))

        val response =
          """{
            |  "utr": "a string representing the tax reference to associate with this internalId",
            |  "managedByAgent": "boolean derived from answers in the claim a estate journey"
            |}""".stripMargin

        wiremock(
          payload = json,
          expectedStatus = Status.CREATED,
          expectedResponse = response
        )

        connector.lock(request) map { response =>
          response.status mustBe CREATED
        }

      }

      "returns 400 BAD_REQUEST" in {

        val json = Json.stringify(Json.toJson(request))

        val response =
          """{
            |  "status": "400",
            |  "message": "Unable to parse request body into a EstateClaim"
            |}""".stripMargin

        wiremock(
          payload = json,
          expectedStatus = Status.BAD_REQUEST,
          expectedResponse = response
        )

        connector.lock(request) map { response =>
          response.status mustBe BAD_REQUEST
        }

      }
      "returns 500 INTERNAL_SERVER_ERROR" in {

        val json = Json.stringify(Json.toJson(request))

        val response =
          """{
            |  "status": "500",
            |  "message": "unable to store to estates store"
            |}""".stripMargin

        wiremock(
          payload = json,
          expectedStatus = Status.INTERNAL_SERVER_ERROR,
          expectedResponse = response
        )

        connector.lock(request) map { response =>
          response.status mustBe INTERNAL_SERVER_ERROR
        }

      }

    }

  }

}
