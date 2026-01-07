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

package views

import views.behaviours.ViewBehaviours
import views.html.EstateLocked

class EstateLockedViewSpec extends ViewBehaviours {

  val utr = "0987654321"

  "EstateLocked view" must {

    val view = viewFor[EstateLocked](Some(emptyUserAnswers))

    val applyView = view.apply(utr)(fakeRequest, messages)

    behave like normalPageWithCaption(
      applyView,
      "locked",
      "utr", utr,
      "p1", "p2", "p3", "p4", "link1"
    )

  }

}
