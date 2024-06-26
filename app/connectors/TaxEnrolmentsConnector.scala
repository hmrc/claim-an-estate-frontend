/*
 * Copyright 2024 HM Revenue & Customs
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

import config.FrontendAppConfig
import javax.inject.Inject
import models.{EnrolmentResponse, TaxEnrolmentRequest}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

class TaxEnrolmentsConnector @Inject()(http: HttpClient, config : FrontendAppConfig) {

  val url: String = config.taxEnrolmentsUrl + s"/service/${config.serviceName}/enrolment"

  def enrol(request: TaxEnrolmentRequest)(implicit hc : HeaderCarrier,
                                          ec : ExecutionContext): Future[EnrolmentResponse] = {

    http.PUT[JsValue, EnrolmentResponse](url, Json.toJson(request))
  }
}
