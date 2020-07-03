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

package controllers

import base.SpecBase
import connectors.{EstatesStoreConnector, RelationshipEstablishmentConnector}
import models.{EstatesStoreRequest, RelationshipEstablishmentStatus}
import navigation.{FakeNavigator, Navigator}
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.{IsAgentManagingEstatePage, UTRPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class IvFailureControllerSpec extends SpecBase {

  lazy val connector: RelationshipEstablishmentConnector = mock[RelationshipEstablishmentConnector]

  "IvFailure Controller" must {

    "callback-failure route" when {

      "redirect to IV FallbackFailure when no journeyId is provided" in {

        val answers = emptyUserAnswers
          .set(UTRPage, "1234567890").success.value
          .set(IsAgentManagingEstatePage, true).success.value

        val fakeNavigator = new FakeNavigator(Call("GET", "/foo"))

        val application = applicationBuilder(userAnswers = Some(answers))
          .overrides(bind[RelationshipEstablishmentConnector].toInstance(connector))
          .overrides(bind[Navigator].toInstance(fakeNavigator))
          .build()

        val onIvFailureRoute = routes.IvFailureController.onEstateIvFailure().url

        val request = FakeRequest(GET, s"$onIvFailureRoute")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.FallbackFailureController.onPageLoad().url

        application.stop()
      }

      "redirect to estate locked page when user fails Estates IV after multiple attempts" in {

        val answers = emptyUserAnswers
          .set(UTRPage, "1234567890").success.value
          .set(IsAgentManagingEstatePage, true).success.value

        val fakeNavigator = new FakeNavigator(Call("GET", "/foo"))

        val onIvFailureRoute = routes.IvFailureController.onEstateIvFailure().url

        val application = applicationBuilder(userAnswers = Some(answers))
          .overrides(bind[RelationshipEstablishmentConnector].toInstance(connector))
          .overrides(bind[Navigator].toInstance(fakeNavigator))
          .build()

        when(connector.journeyId(any[String])(any(), any()))
          .thenReturn(Future.successful(RelationshipEstablishmentStatus.Locked))

        val request = FakeRequest(GET, s"$onIvFailureRoute?journeyId=47a8a543-6961-4221-86e8-d22e2c3c91de")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.IvFailureController.estateLocked().url

        application.stop()
      }

      "redirect to estate utr not found page when the utr isn't found" in {

        val answers = emptyUserAnswers
          .set(UTRPage, "1234567890").success.value
          .set(IsAgentManagingEstatePage, true).success.value

        val application = applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[RelationshipEstablishmentConnector].toInstance(connector)
          )
          .build()

        when(connector.journeyId(any[String])(any(), any()))
          .thenReturn(Future.successful(RelationshipEstablishmentStatus.NotFound))

        val onIvFailureRoute = routes.IvFailureController.onEstateIvFailure().url

        val request = FakeRequest(GET, s"$onIvFailureRoute?journeyId=47a8a543-6961-4221-86e8-d22e2c3c91de")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.IvFailureController.estateNotFound().url

        application.stop()
      }

      "redirect to estate utr in processing page when the utr is processing" in {

        val answers = emptyUserAnswers
          .set(UTRPage, "1234567890").success.value
          .set(IsAgentManagingEstatePage, true).success.value

        val application = applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[RelationshipEstablishmentConnector].toInstance(connector)
          )
          .build()

        when(connector.journeyId(any[String])(any(), any()))
          .thenReturn(Future.successful(RelationshipEstablishmentStatus.InProcessing))

        val onIvFailureRoute = routes.IvFailureController.onEstateIvFailure().url

        val request = FakeRequest(GET, s"$onIvFailureRoute?journeyId=47a8a543-6961-4221-86e8-d22e2c3c91de")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.IvFailureController.estateStillProcessing().url

        application.stop()
      }
    }

    "locked route" when {

      "return OK and the correct view for a GET for locked route" in {

        val fakeNavigator = new FakeNavigator(Call("GET", "/foo"))

        val onLockedRoute = routes.IvFailureController.estateLocked().url
        val utr = "3000000001"
        val managedByAgent = true
        val estateLocked = true

        val connector = mock[EstatesStoreConnector]

        when(connector.lock(eqTo(EstatesStoreRequest(userAnswersId, utr, managedByAgent, estateLocked)))(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(CREATED)))

        val answers = emptyUserAnswers
          .set(UTRPage, utr).success.value
          .set(IsAgentManagingEstatePage, true).success.value

        val application = applicationBuilder(userAnswers = Some(answers))
          .overrides(bind[EstatesStoreConnector].toInstance(connector))
          .overrides(bind[Navigator].toInstance(fakeNavigator))
          .build()

        val request = FakeRequest(GET, onLockedRoute)

        val result = route(application, request).value

        status(result) mustEqual OK

        contentAsString(result) must include("As you have had 3 unsuccessful tries at accessing this estate you will need to try again in 30 minutes.")

        verify(connector).lock(eqTo(EstatesStoreRequest(userAnswersId, utr, managedByAgent, estateLocked)))(any(), any(), any())

        application.stop()
      }

      "return OK and the correct view for a GET for not found route" in {

        val onLockedRoute = routes.IvFailureController.estateNotFound().url

        val answers = emptyUserAnswers
          .set(UTRPage, "1234567890").success.value

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        val request = FakeRequest(GET, onLockedRoute)

        val result = route(application, request).value

        status(result) mustEqual OK

        contentAsString(result) must include("The Unique Taxpayer Reference (UTR) you gave for the estate does not match our records")

        application.stop()
      }

      "return OK and the correct view for a GET for still processing route" in {

        val onLockedRoute = routes.IvFailureController.estateStillProcessing().url

        val answers = emptyUserAnswers
          .set(UTRPage, "1234567891").success.value

        val application = applicationBuilder(userAnswers = Some(answers)).build()

        val request = FakeRequest(GET, onLockedRoute)

        val result = route(application, request).value

        status(result) mustEqual OK

        application.stop()
      }

      "redirect to Session Expired for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        val onLockedRoute = routes.IvFailureController.estateLocked().url

        val request = FakeRequest(GET, onLockedRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.SessionExpiredController.onPageLoad().url

        application.stop()
      }

    }

  }
}