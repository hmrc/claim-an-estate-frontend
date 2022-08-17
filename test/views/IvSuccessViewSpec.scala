/*
 * Copyright 2022 HM Revenue & Customs
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

package views

import views.behaviours.ViewBehaviours
import views.html.IvSuccessView

class IvSuccessViewSpec extends ViewBehaviours {

  val utr = "0987654321"

  "IvSuccess view with Agent" must {

    "display the register link when config.playbackEnabled is true" when {

      val view = viewFor[IvSuccessView](Some(emptyUserAnswers))

      val applyView = view.apply(isAgent = true, utr)(fakeRequest, messages)

      behave like normalPageWithCaption(
        applyView,
        "ivSuccess.agent",
        "utr", utr,
        "paragraph1", "paragraph2", "paragraph3", "paragraph4", "paragraph5"
      )
    }

    "display the correct subheading" in {

      val view = viewFor[IvSuccessView](Some(emptyUserAnswers))

      val applyView = view.apply(isAgent = true, utr)(fakeRequest, messages)

      val doc = asDocument(applyView)
      assertContainsText(doc, messages("utr.subheading", utr))
    }

  }

  "IvSuccess view with no Agent" must {

    "render view" when {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .configure("microservice.services.features.playback.enabled" -> true)
        .build()

      val view = application.injector.instanceOf[IvSuccessView]

      val applyView = view.apply(isAgent = false, utr)(fakeRequest, messages)

      behave like normalPageWithCaption(applyView, "ivSuccess.no.agent", "utr", utr, "paragraph1", "paragraph2")

      "display the correct subheading" in {
        val doc = asDocument(applyView)
        assertContainsText(doc, messages("utr.subheading", utr))
      }

      "show the continue button" in {
        val doc = asDocument(applyView)
        assertRenderedByCssSelector(doc, ".button")
      }

    }


  }

}
