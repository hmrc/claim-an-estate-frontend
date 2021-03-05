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
import models.{EnrolmentCreated, TaxEnrolmentRequest, UserAnswers}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.{HasEnrolled, IsAgentManagingEstatePage, UTRPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.{RelationshipEstablishment, RelationshipFound}
import uk.gov.hmrc.http.BadRequestException
import views.html.IvSuccessView

import scala.concurrent.Future

class IvSuccessControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val utr = "0987654321"

  private val connector = mock[TaxEnrolmentsConnector]
  private val mockRelationshipEstablishment = mock[RelationshipEstablishment]

  // Mock mongo repository
  private val mockRepository = mock[SessionRepository]

  override def beforeEach {
    reset(connector)
    reset(mockRelationshipEstablishment)
    reset(mockRepository)
    super.beforeEach()
  }

  "IvSuccess Controller" must {

    "claiming an estate" must {

      "return OK and the correct view for a GET with no Agent and set hasEnrolled true" in {

        val userAnswers = UserAnswers(userAnswersId)
          .set(IsAgentManagingEstatePage, false).success.value
          .set(UTRPage, utr).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers), relationshipEstablishment = mockRelationshipEstablishment)
          .overrides(
            bind(classOf[TaxEnrolmentsConnector]).toInstance(connector),
            bind(classOf[SessionRepository]).toInstance(mockRepository)
          )
          .build()

        val request = FakeRequest(GET, routes.IvSuccessController.onPageLoad().url)

        val view = application.injector.instanceOf[IvSuccessView]

        val viewAsString = view(isAgent = false, utr)(request, messages).toString

        // Stub a mongo connection
        when(mockRepository.set(any())).thenReturn(Future.successful(true))

        when(mockRelationshipEstablishment.check(eqTo("id"), eqTo(utr))(any()))
          .thenReturn(Future.successful(RelationshipFound))

        when(connector.enrol(eqTo(TaxEnrolmentRequest(utr)))(any(), any()))
          .thenReturn(Future.successful(EnrolmentCreated))

        val result = route(application, request).value

        status(result) mustEqual OK

        contentAsString(result) mustEqual viewAsString

        // Verify if the HasEnrolled value is being set in mongo
        val userAnswersWithHasEnrolled = userAnswers.set(HasEnrolled, true).success.value
        verify(mockRepository, times(1)).set(eqTo(userAnswersWithHasEnrolled))

        verify(connector, atLeastOnce()).enrol(eqTo(TaxEnrolmentRequest(utr)))(any(), any())
        verify(mockRelationshipEstablishment).check(eqTo("id"), eqTo(utr))(any())
        verify(mockAuditService).auditEstateClaimed(eqTo(utr), eqTo("id"))(any())

        application.stop()

      }

      "return OK and the correct view for a GET with Agent and set hasEnrolled true" in {

        val userAnswers = UserAnswers(userAnswersId)
          .set(IsAgentManagingEstatePage, true).success.value
          .set(UTRPage, utr).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers), relationshipEstablishment = mockRelationshipEstablishment)
          .overrides(
            bind(classOf[TaxEnrolmentsConnector]).toInstance(connector),
            bind(classOf[SessionRepository]).toInstance(mockRepository)
          )
          .build()

        val request = FakeRequest(GET, routes.IvSuccessController.onPageLoad().url)

        val view = application.injector.instanceOf[IvSuccessView]

        val viewAsString = view(isAgent = true, utr)(request, messages).toString

        // Stub a mongo connection
        when(mockRepository.set(any())).thenReturn(Future.successful(true))

        when(mockRelationshipEstablishment.check(eqTo("id"), eqTo(utr))(any()))
          .thenReturn(Future.successful(RelationshipFound))

        when(connector.enrol(eqTo(TaxEnrolmentRequest(utr)))(any(), any()))
          .thenReturn(Future.successful(EnrolmentCreated))

        val result = route(application, request).value

        status(result) mustEqual OK

        contentAsString(result) mustEqual viewAsString

        // Verify if the HasEnrolled value is being set in mongo
        val userAnswersWithHasEnrolled = userAnswers.set(HasEnrolled, true).success.value
        verify(mockRepository, times(1)).set(eqTo(userAnswersWithHasEnrolled))

        verify(connector, atLeastOnce()).enrol(eqTo(TaxEnrolmentRequest(utr)))(any(), any())
        verify(mockRelationshipEstablishment).check(eqTo("id"), eqTo(utr))(any())
        verify(mockAuditService).auditEstateClaimed(eqTo(utr), eqTo("id"))(any())

        application.stop()

      }

    }

    "claiming an estate after failure or reload" must {

      "return OK and the correct view for a GET with no Agent and set hasEnrolled true" in {

        val userAnswers = UserAnswers(userAnswersId)
          .set(IsAgentManagingEstatePage, false).success.value
          .set(UTRPage, utr).success.value
          .set(HasEnrolled, false).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers), relationshipEstablishment = mockRelationshipEstablishment)
          .overrides(
            bind(classOf[TaxEnrolmentsConnector]).toInstance(connector),
            bind(classOf[SessionRepository]).toInstance(mockRepository)
          )
          .build()

        val request = FakeRequest(GET, routes.IvSuccessController.onPageLoad().url)

        val view = application.injector.instanceOf[IvSuccessView]

        val viewAsString = view(isAgent = false, utr)(request, messages).toString

        // Stub a mongo connection
        when(mockRepository.set(any())).thenReturn(Future.successful(true))

        when(mockRelationshipEstablishment.check(eqTo("id"), eqTo(utr))(any()))
          .thenReturn(Future.successful(RelationshipFound))

        when(connector.enrol(eqTo(TaxEnrolmentRequest(utr)))(any(), any()))
          .thenReturn(Future.successful(EnrolmentCreated))

        val result = route(application, request).value

        status(result) mustEqual OK

        contentAsString(result) mustEqual viewAsString

        // Verify if the HasEnrolled value is being set in mongo
        val userAnswersWithHasEnrolled = userAnswers.set(HasEnrolled, true).success.value
        verify(mockRepository, times(1)).set(eqTo(userAnswersWithHasEnrolled))

        verify(connector).enrol(eqTo(TaxEnrolmentRequest(utr)))(any(), any())
        verify(mockRelationshipEstablishment).check(eqTo("id"), eqTo(utr))(any())
        verify(mockAuditService).auditEstateClaimed(eqTo(utr), eqTo("id"))(any())

        application.stop()

      }

      "return OK and the correct view for a GET with Agent and set hasEnrolled true" in {

        val userAnswers = UserAnswers(userAnswersId)
          .set(IsAgentManagingEstatePage, true).success.value
          .set(UTRPage, utr).success.value
          .set(HasEnrolled, false).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers), relationshipEstablishment = mockRelationshipEstablishment)
          .overrides(
            bind(classOf[TaxEnrolmentsConnector]).toInstance(connector),
            bind(classOf[SessionRepository]).toInstance(mockRepository)
          )
          .build()

        val request = FakeRequest(GET, routes.IvSuccessController.onPageLoad().url)

        val view = application.injector.instanceOf[IvSuccessView]

        val viewAsString = view(isAgent = true, utr)(request, messages).toString

        // Stub a mongo connection
        when(mockRepository.set(any())).thenReturn(Future.successful(true))

        when(mockRelationshipEstablishment.check(eqTo("id"), eqTo(utr))(any()))
          .thenReturn(Future.successful(RelationshipFound))

        when(connector.enrol(eqTo(TaxEnrolmentRequest(utr)))(any(), any()))
          .thenReturn(Future.successful(EnrolmentCreated))

        val result = route(application, request).value

        status(result) mustEqual OK

        contentAsString(result) mustEqual viewAsString

        // Verify if the HasEnrolled value is being set in mongo
        val userAnswersWithHasEnrolled = userAnswers.set(HasEnrolled, true).success.value
        verify(mockRepository, times(1)).set(eqTo(userAnswersWithHasEnrolled))

        verify(connector).enrol(eqTo(TaxEnrolmentRequest(utr)))(any(), any())
        verify(mockRelationshipEstablishment).check(eqTo("id"), eqTo(utr))(any())
        verify(mockAuditService).auditEstateClaimed(eqTo(utr), eqTo("id"))(any())

        application.stop()

      }

    }

    "rendering page after having claimed" must {

      "return OK and the correct view for a GET with no Agent and has enrolled" in {

        val userAnswers = UserAnswers(userAnswersId)
          .set(IsAgentManagingEstatePage, false).success.value
          .set(UTRPage, utr).success.value
          .set(HasEnrolled, true).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers), relationshipEstablishment = mockRelationshipEstablishment)
          .overrides(
            bind(classOf[TaxEnrolmentsConnector]).toInstance(connector),
            bind(classOf[SessionRepository]).toInstance(mockRepository)
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

        verify(mockRepository, never()).set(any())
        verify(connector, never()).enrol(eqTo(TaxEnrolmentRequest(utr)))(any(), any())
        verify(mockRelationshipEstablishment).check(eqTo("id"), eqTo(utr))(any())
        verify(mockAuditService).auditEstateClaimed(eqTo(utr), eqTo("id"))(any())

        application.stop()

      }

      "return OK and the correct view for a GET with Agent and has enrolled" in {

        val userAnswers = UserAnswers(userAnswersId)
          .set(IsAgentManagingEstatePage, true).success.value
          .set(UTRPage, utr).success.value
          .set(HasEnrolled, true).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers), relationshipEstablishment = mockRelationshipEstablishment)
          .overrides(
            bind(classOf[TaxEnrolmentsConnector]).toInstance(connector),
            bind(classOf[SessionRepository]).toInstance(mockRepository)
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

        verify(mockRepository, never()).set(any())
        verify(connector, never()).enrol(eqTo(TaxEnrolmentRequest(utr)))(any(), any())
        verify(mockRelationshipEstablishment).check(eqTo("id"), eqTo(utr))(any())
        verify(mockAuditService).auditEstateClaimed(eqTo(utr), eqTo("id"))(any())

        application.stop()

      }

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

            val utr = "1234567890"

            val userAnswers = UserAnswers(userAnswersId)
              .set(IsAgentManagingEstatePage, true).success.value
              .set(UTRPage, utr).success.value

            val application = applicationBuilder(userAnswers = Some(userAnswers), relationshipEstablishment = mockRelationshipEstablishment)
              .overrides(
                bind(classOf[TaxEnrolmentsConnector]).toInstance(connector),
                bind(classOf[SessionRepository]).toInstance(mockRepository)
              )
              .build()

            val request = FakeRequest(GET, routes.IvSuccessController.onPageLoad().url)

            // Stub a mongo connection
            when(mockRepository.set(any())).thenReturn(Future.successful(true))

            when(mockRelationshipEstablishment.check(eqTo("id"), eqTo(utr))(any()))
              .thenReturn(Future.successful(RelationshipFound))

            when(connector.enrol(eqTo(TaxEnrolmentRequest(utr)))(any(), any()))
              .thenReturn(Future.failed(new Exception("bad juju")))

            val result = route(application, request).value

            status(result) mustEqual INTERNAL_SERVER_ERROR

            // Verify if the HasEnrolled value is being unset in mongo in case of errors
            val userAnswersWithHasEnrolledUnset = userAnswers.set(HasEnrolled, false).success.value
            verify(mockRepository, times(1)).set(eqTo(userAnswersWithHasEnrolledUnset))

            verify(connector).enrol(eqTo(TaxEnrolmentRequest(utr)))(any(), any())
            verify(mockRelationshipEstablishment).check(eqTo("id"), eqTo(utr))(any())
            verify(mockAuditService).auditEstateClaimError(eqTo(utr), eqTo("id"), eqTo("bad juju"))(any())

            application.stop()

          }
          "400 BAD_REQUEST" in {

            val utr = "0987654321"

            val userAnswers = UserAnswers(userAnswersId)
              .set(IsAgentManagingEstatePage, true).success.value
              .set(UTRPage, utr).success.value

            val application = applicationBuilder(userAnswers = Some(userAnswers), relationshipEstablishment = mockRelationshipEstablishment)
              .overrides(
                bind(classOf[TaxEnrolmentsConnector]).toInstance(connector),
                bind(classOf[SessionRepository]).toInstance(mockRepository)
              )
              .build()

            val request = FakeRequest(GET, routes.IvSuccessController.onPageLoad().url)

            // Stub a mongo connection
            when(mockRepository.set(any())).thenReturn(Future.successful(true))

            when(mockRelationshipEstablishment.check(eqTo("id"), eqTo(utr))(any()))
              .thenReturn(Future.successful(RelationshipFound))

            when(connector.enrol(eqTo(TaxEnrolmentRequest(utr)))(any(), any()))
              .thenReturn(Future.failed(new BadRequestException("DoublePlusUngood")))

            val result = route(application, request).value

            status(result) mustEqual INTERNAL_SERVER_ERROR

            // Verify if the HasEnrolled value is being unset in mongo in case of errors
            val userAnswersWithHasEnrolledUnset = userAnswers.set(HasEnrolled, false).success.value
            verify(mockRepository, times(1)).set(eqTo(userAnswersWithHasEnrolledUnset))

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
