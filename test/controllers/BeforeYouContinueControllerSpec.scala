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
import connectors.EstatesStoreConnector
import models.EstatesStoreRequest
import navigation.{FakeNavigator, Navigator}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.{IsAgentManagingEstatePage, UTRPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{FakeRelationshipEstablishmentService, RelationshipNotFound}
import uk.gov.hmrc.http.HttpResponse
import views.html.BeforeYouContinueView

import scala.concurrent.Future

class BeforeYouContinueControllerSpec extends SpecBase {

  val utr = "0987654321"
  val managedByAgent = true
  val estateLocked = false

  val fakeEstablishmentServiceFailing = new FakeRelationshipEstablishmentService(RelationshipNotFound)

  "BeforeYouContinue Controller" must {

    "return OK and the correct view for a GET" in {

      val answers = emptyUserAnswers.set(UTRPage, utr).success.value

      val application = applicationBuilder(userAnswers = Some(answers), fakeEstablishmentServiceFailing).build()

      val request = FakeRequest(GET, routes.BeforeYouContinueController.onPageLoad().url)

      val result = route(application, request).value

      val view = application.injector.instanceOf[BeforeYouContinueView]

      status(result) mustEqual OK

      contentAsString(result) mustEqual
        view(utr)(request, messages).toString

      application.stop()
    }

    "redirect to relationship establishment for a POST" in {

      val fakeNavigator = new FakeNavigator(Call("GET", "/foo"))

      val connector = mock[EstatesStoreConnector]

      when(connector.lock(eqTo(EstatesStoreRequest(userAnswersId, utr, managedByAgent, estateLocked)))(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(CREATED, "")))

      val answers = emptyUserAnswers
        .set(UTRPage, "0987654321").success.value
        .set(IsAgentManagingEstatePage, true).success.value

      val application = applicationBuilder(userAnswers = Some(answers), fakeEstablishmentServiceFailing)
        .overrides(bind[EstatesStoreConnector].toInstance(connector))
        .overrides(bind[Navigator].toInstance(fakeNavigator))
        .build()

      val request = FakeRequest(POST, routes.BeforeYouContinueController.onSubmit().url)

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value must include("0987654321")

      verify(connector).lock(eqTo(EstatesStoreRequest(userAnswersId, utr, managedByAgent, estateLocked)))(any(), any(), any())

      application.stop()

    }
  }
}
