/*
 * Copyright 2020 HM Revenue & Customs
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
import config.FrontendAppConfig
import models.{EnrolmentCreated, EnrolmentFailed, TaxEnrolmentRequest}
import org.scalatest.{AsyncWordSpec, MustMatchers, RecoverMethods}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.WireMockHelper

class TaxEnrolmentsConnectorSpec extends AsyncWordSpec with MustMatchers with WireMockHelper with RecoverMethods {

  private implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  private lazy val app = new GuiceApplicationBuilder()
    .configure(Seq(
      "microservice.services.tax-enrolments.port" -> server.port(),
      "auditing.enabled" -> false): _*
    )
    .build()

  private lazy val config: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  private lazy val connector: TaxEnrolmentsConnector = app.injector.instanceOf[TaxEnrolmentsConnector]

  private lazy val url: String = s"/tax-enrolments/service/${config.serviceName}/enrolment"

  private val utr = "1234567890"

  private val request = Json.stringify(Json.obj(
    "identifiers" -> Json.arr(
      Json.obj(
        "key" -> "SAUTR",
        "value" -> utr
      )),
    "verifiers" -> Json.arr(
      Json.obj(
        "key" -> "SAUTR1",
        "value" -> utr
      )
    )
  ))

  private def wiremock(payload: String, simulatedStatus: Int, simulatedResponse: String) =
    server.stubFor(
      put(urlEqualTo(url))
        .withHeader(CONTENT_TYPE, containing("application/json"))
        .withRequestBody(equalTo(payload))
        .willReturn(
          aResponse()
            .withStatus(simulatedStatus)
            .withBody(simulatedResponse)
        )
    )

  "TaxEnrolmentsConnector" must {

    "call PUT /" which {

      "returns 204 NO_CONTENT" in {

        wiremock(
          payload = request,
          simulatedStatus = NO_CONTENT,
          simulatedResponse = ""
        )

        connector.enrol(TaxEnrolmentRequest(utr)) map { response =>
          response mustBe EnrolmentCreated
        }

      }

      "returns other status" in {

        wiremock(
          payload = request,
          simulatedStatus = BAD_REQUEST,
          simulatedResponse = "error body"
        )

        connector.enrol(TaxEnrolmentRequest(utr)) map { response =>
          response mustBe EnrolmentFailed(BAD_REQUEST, "error body")
        }
      }
    }
  }
}
