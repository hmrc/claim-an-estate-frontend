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

package services

import base.SpecBase
import config.FrontendAppConfig
import models.EnrolmentFailed
import models.auditing.AuditData
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class AuditServiceSpec extends SpecBase with MockitoSugar {

  private val mockAuditConnector         = mock[AuditConnector]
  lazy val config: FrontendAppConfig     = app.injector.instanceOf[FrontendAppConfig]
  private val auditService: AuditService = new AuditService(mockAuditConnector)

  private given hc: HeaderCarrier = HeaderCarrier()

  private val utr        = "1234567890"
  private val internalId = "internal-auth-id-123"

  "AuditService" should {

    "auditEstateClaimed" should {
      "send an explicit audit event with the correct payload" in {
        reset(mockAuditConnector)

        auditService.auditEstateClaimed(utr, internalId)

        val expectedPayload = AuditData(
          request = Json.obj("utr" -> utr),
          internalAuthId = internalId,
          response = Some(Json.obj())
        )

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(
            auditType = "EstateClaimed",
            detail = expectedPayload
          )
      }
    }

    "auditEstateClaimFailed" should {
      "send an explicit audit event with the failure reason in the response payload" in {
        reset(mockAuditConnector)

        val failedEnrolment = EnrolmentFailed(1, "An error occurred during enrolment")

        auditService.auditEstateClaimFailed(utr, internalId, failedEnrolment)

        val expectedPayload = AuditData(
          request = Json.obj("utr" -> utr),
          internalAuthId = internalId,
          response = Some(Json.toJson(failedEnrolment))
        )

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(
            auditType = "EstateClaimFailed",
            detail = expectedPayload
          )
      }
    }

    "auditEstateClaimError" should {
      "send an explicit audit event containing the literal error string" in {
        reset(mockAuditConnector)

        val errorReason = "Connection timed out"

        auditService.auditEstateClaimError(utr, internalId, errorReason)

        val expectedPayload = AuditData(
          request = Json.obj("utr" -> utr),
          internalAuthId = internalId,
          response = Some(Json.obj("errorReason" -> errorReason))
        )

        verify(mockAuditConnector, times(1))
          .sendExplicitAudit(
            auditType = "EstateClaimError",
            detail = expectedPayload
          )
      }
    }
  }

}
