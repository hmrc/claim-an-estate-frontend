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

package controllers

import base.SpecBase
import connectors.TaxEnrolmentsConnector
import models.{EnrolmentCreated, EnrolmentFailed, TaxEnrolmentRequest, UserAnswers}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.{IsAgentManagingEstatePage, UTRPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{RelationshipEstablishment, RelationshipFound}
import uk.gov.hmrc.http.BadRequestException
import views.html.IvSuccessView

import scala.concurrent.Future

class IvSuccessControllerSpec extends SpecBase with BeforeAndAfterAll {

  private val utr = "0987654321"

  private val connector = mock[TaxEnrolmentsConnector]
  private val mockRelationshipEstablishment = mock[RelationshipEstablishment]

  "IvSuccess Controller" must {

    "return OK and the correct view for a GET with no Agent" in {

      reset(connector, mockRelationshipEstablishment)

      val userAnswers = UserAnswers(userAnswersId)
        .set(IsAgentManagingEstatePage, false).success.value
        .set(UTRPage, utr).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), relationshipEstablishment = mockRelationshipEstablishment)
        .overrides(
          bind(classOf[TaxEnrolmentsConnector]).toInstance(connector)
        )
        .build()

      val request = FakeRequest(GET, routes.IvSuccessController.onPageLoad().url)

      val view = application.injector.instanceOf[IvSuccessView]

      val viewAsString = view(isAgent = false, utr)(request, messages).toString

      when(mockRelationshipEstablishment.check(eqTo("id"), eqTo(utr))(any()))
        .thenReturn(Future.successful(RelationshipFound))

      when(connector.enrol(eqTo(TaxEnrolmentRequest(utr)))(any(), any()))
        .thenReturn(Future.successful(EnrolmentCreated))

      val result = route(application, request).value

      status(result) mustEqual OK

      contentAsString(result) mustEqual viewAsString

      verify(connector).enrol(eqTo(TaxEnrolmentRequest(utr)))(any(), any())
      verify(mockRelationshipEstablishment).check(eqTo("id"), eqTo(utr))(any())
      verify(mockAuditService).auditEstateClaimed(eqTo(utr), eqTo("id"))(any())

      application.stop()

    }

    "return OK and the correct view for a GET with Agent" in {

      reset(connector, mockRelationshipEstablishment)

      val userAnswers = UserAnswers(userAnswersId)
        .set(IsAgentManagingEstatePage, true).success.value
        .set(UTRPage, utr).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), relationshipEstablishment = mockRelationshipEstablishment)
        .overrides(
          bind(classOf[TaxEnrolmentsConnector]).toInstance(connector)
        )
        .build()

      val request = FakeRequest(GET, routes.IvSuccessController.onPageLoad().url)

      val view = application.injector.instanceOf[IvSuccessView]

      val viewAsString = view(isAgent = true, utr)(request, messages).toString

      when(mockRelationshipEstablishment.check(eqTo("id"), eqTo(utr))(any()))
        .thenReturn(Future.successful(RelationshipFound))

      when(connector.enrol(eqTo(TaxEnrolmentRequest(utr)))(any(), any()))
        .thenReturn(Future.successful(EnrolmentCreated))

      val result = route(application, request).value

      status(result) mustEqual OK

      contentAsString(result) mustEqual viewAsString

      verify(connector).enrol(eqTo(TaxEnrolmentRequest(utr)))(any(), any())
      verify(mockRelationshipEstablishment).check(eqTo("id"), eqTo(utr))(any())
      verify(mockAuditService).auditEstateClaimed(eqTo(utr), eqTo("id"))(any())

      application.stop()

    }

    "redirect to Session Expired" when {

      "no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        val request = FakeRequest(GET, routes.IvSuccessController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.SessionExpiredController.onPageLoad().url
        verify(mockAuditService).auditEstateClaimError(eqTo("Unknown"), eqTo("id"), eqTo("No UTR available on success"))(any())

        application.stop()
      }

      "redirect to Internal Server Error" when {

        "tax enrolments fails" when {

          "401 UNAUTHORIZED" in {

            reset(connector, mockRelationshipEstablishment)

            val utr = "1234567890"

            val userAnswers = UserAnswers(userAnswersId)
              .set(IsAgentManagingEstatePage, true).success.value
              .set(UTRPage, utr).success.value

            val application = applicationBuilder(userAnswers = Some(userAnswers), relationshipEstablishment = mockRelationshipEstablishment)
              .overrides(
                bind(classOf[TaxEnrolmentsConnector]).toInstance(connector)
              )
              .build()

            val request = FakeRequest(GET, routes.IvSuccessController.onPageLoad().url)

            when(mockRelationshipEstablishment.check(eqTo("id"), eqTo(utr))(any()))
              .thenReturn(Future.successful(RelationshipFound))

            when(connector.enrol(any())(any(), any()))
              .thenReturn(Future.successful(EnrolmentFailed(BAD_REQUEST, "bad juju")))

            val result = route(application, request).value

            status(result) mustEqual INTERNAL_SERVER_ERROR

            verify(connector).enrol(eqTo(TaxEnrolmentRequest(utr)))(any(), any())
            verify(mockRelationshipEstablishment).check(eqTo("id"), eqTo(utr))(any())
            verify(mockAuditService).auditEstateClaimFailed(eqTo(utr), eqTo("id"), eqTo(EnrolmentFailed(BAD_REQUEST, "bad juju")))(any())

            application.stop()

          }
          "400 BAD_REQUEST" in {

            reset(connector, mockRelationshipEstablishment)

            val utr = "0987654321"

            val userAnswers = UserAnswers(userAnswersId)
              .set(IsAgentManagingEstatePage, true).success.value
              .set(UTRPage, utr).success.value

            val application = applicationBuilder(userAnswers = Some(userAnswers), relationshipEstablishment = mockRelationshipEstablishment)
              .overrides(
                bind(classOf[TaxEnrolmentsConnector]).toInstance(connector)
              )
              .build()

            val request = FakeRequest(GET, routes.IvSuccessController.onPageLoad().url)

            when(mockRelationshipEstablishment.check(eqTo("id"), eqTo(utr))(any()))
              .thenReturn(Future.successful(RelationshipFound))

            when(connector.enrol(eqTo(TaxEnrolmentRequest(utr)))(any(), any()))
              .thenReturn(Future.failed(new BadRequestException("DoublePlusUngood")))

            val result = route(application, request).value

            status(result) mustEqual INTERNAL_SERVER_ERROR

            verify(connector).enrol(eqTo(TaxEnrolmentRequest(utr)))(any(), any())
            verify(mockRelationshipEstablishment).check(eqTo("id"), eqTo(utr))(any())
            verify(mockAuditService).auditEstateClaimError(eqTo(utr), eqTo("id"), eqTo("DoublePlusUngood"))(any())

            application.stop()

          }
        }
      }
    }
  }
}
