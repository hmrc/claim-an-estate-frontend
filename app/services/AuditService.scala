/*
 * Copyright 2021 HM Revenue & Customs
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

package services

import javax.inject.Inject
import models.EnrolmentFailed
import models.auditing.AuditData
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext

class AuditService @Inject()(auditConnector: AuditConnector)(implicit ec: ExecutionContext) {

  private object AuditEvent {
    val ESTATE_CLAIMED = "EstateClaimed"
    val ESTATE_CLAIM_FAILED = "EstateClaimFailed"
    val ESTATE_CLAIM_ERROR = "EstateClaimError"
  }

  def auditEstateClaimed(utr: String, internalId: String)(implicit hc: HeaderCarrier): Unit =
    audit(
      AuditEvent.ESTATE_CLAIMED,
      Json.obj("utr" -> utr),
      internalId,
      Json.obj()
    )

  def auditEstateClaimFailed(utr: String, internalId: String, failed: EnrolmentFailed)(implicit hc: HeaderCarrier): Unit =
    audit(
      AuditEvent.ESTATE_CLAIM_FAILED,
      Json.obj("utr" -> utr),
      internalId,
      Json.toJson(failed)
    )

  def auditEstateClaimError(utr: String, internalId: String, errorReason: String)(implicit hc: HeaderCarrier): Unit =
    audit(
      AuditEvent.ESTATE_CLAIM_ERROR,
      Json.obj("utr" -> utr),
      internalId,
      Json.obj("errorReason" -> errorReason)
    )

  private def audit(event: String, request: JsValue, internalId: String, response: JsValue)
                   (implicit hc: HeaderCarrier): Unit = {

    val auditPayload = AuditData(
      request = request,
      internalAuthId = internalId,
      response = Some(response)
    )

    auditConnector.sendExplicitAudit(
      event,
      auditPayload
    )
  }
}
