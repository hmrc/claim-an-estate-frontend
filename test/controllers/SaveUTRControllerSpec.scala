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

package controllers

import base.SpecBase
import models.{NormalMode, UserAnswers}
import org.mockito.Mockito.{reset, when}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.UTRPage
import play.api.Logger
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.{FakeRelationshipEstablishmentService, RelationshipFound, RelationshipNotFound}
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

import scala.concurrent.Future

class SaveUTRControllerSpec extends SpecBase with LogCapturing with BeforeAndAfterEach {

  val utr = "0987654321"

  val fakeEstablishmentServiceFailing = new FakeRelationshipEstablishmentService(RelationshipNotFound)
  val fakeEstablishmentServicePassing = new FakeRelationshipEstablishmentService(RelationshipFound)

  private val mockSessionRepository = mock[SessionRepository]

  private val appWithFailingRelationship =
    applicationBuilder(userAnswers = None, fakeEstablishmentServiceFailing)
      .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
      .build()

  private val appWithPassingRelationship =
    applicationBuilder(userAnswers = None, fakeEstablishmentServicePassing)
      .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
      .build()

  private val appWithExistingAnswersAndFailingRelationship =
    applicationBuilder(userAnswers = Some(emptyUserAnswers), fakeEstablishmentServiceFailing)
      .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
      .build()

  val request = FakeRequest(GET, routes.SaveUTRController.save(utr).url)

  override def beforeEach(): Unit = {
    reset(mockSessionRepository)
    super.beforeEach()
  }

  "SaveUTRController" must {

    "send UTR to session repo" when {

      "user answers does not exist" in {
        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])

        when(mockSessionRepository.set(captor.capture())).thenReturn(Future.successful(true))

        val result = route(appWithFailingRelationship, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe routes.IsAgentManagingEstateController.onPageLoad(NormalMode).url

        captor.getValue.get(UTRPage).value mustBe utr
      }

      "user answers exists" in
        withCaptureOfLoggingFrom(Logger(classOf[SaveUTRController])) { logs =>
          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])

          when(mockSessionRepository.set(captor.capture())).thenReturn(Future.successful(true))

          val result = route(appWithExistingAnswersAndFailingRelationship, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustBe routes.IsAgentManagingEstateController.onPageLoad(NormalMode).url

          captor.getValue.get(UTRPage).value mustBe utr

          logs.size mustEqual 1
          logs.head.toString must include("user has started the claim an estate journey for utr 0987654321")
        }

      "user answers exists and relationship found" in
        withCaptureOfLoggingFrom(Logger(classOf[SaveUTRController])) { logs =>
          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])

          when(mockSessionRepository.set(captor.capture())).thenReturn(Future.successful(true))

          val result = route(appWithPassingRelationship, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustBe routes.IvSuccessController.onPageLoad.url

          logs.size mustEqual 1
          logs.head.toString must include(
            "relationship is already established in IV for utr 0987654321 sending user to successfully claimed"
          )

          Mockito.verifyNoInteractions(mockSessionRepository)
        }
    }

    "return an internal server error" when {

      "UTR fails validation" in
        withCaptureOfLoggingFrom(Logger(classOf[SaveUTRController])) { logs =>
          val request = FakeRequest(GET, controllers.routes.SaveUTRController.save("<script>alert('xss')</script>").url)

          val result = route(appWithFailingRelationship, request).value

          status(result) mustEqual INTERNAL_SERVER_ERROR

          logs.size mustEqual 1
          logs.head.toString must include("Invalid UTR: <script>alert('xss')</script>")

          Mockito.verifyNoInteractions(mockSessionRepository)
        }
    }

  }

}
