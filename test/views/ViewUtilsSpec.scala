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

import org.scalatest.matchers.should.Matchers.*
import play.api.data.FormError
import viewmodels.RadioOption

class ViewUtilsSpec extends ViewSpecBase {

  "View utils" must {

    "render the subheading for a utr" in {
      val heading = ViewUtils.subheading("1234567890")
      heading mustBe "The estate’s UTR: 1234567890"
    }

  }

  " breadcrumbTitle" should {
    "return the expected breadcrumb title" in {
      val heading = ViewUtils.breadcrumbTitle("1234567890")
      heading mustBe "1234567890 - Manage an estate - GOV.UK"
    }
  }

  "errorHref" should {
    "return the correct field identifier for single date fields" in {
      val error  = FormError("startDate", "error.date.yesno")
      val result = ViewUtils.errorHref(error, Nil)
      result shouldBe "startDate-yes"
    }

    "handle errorHref  with default radioOptions parameter" in {
      val error  = FormError("startDate", "some.error")
      val result = ViewUtils.errorHref(error)
      result shouldBe "startDate.day"
    }

    "handle non-empty radio options correctly" in {
      val error        = FormError("someField", "some.error")
      val radioOptions = Seq(RadioOption("id", "value"))
      val result       = ViewUtils.errorHref(error, radioOptions)
      result shouldBe "id.value"
    }
    "handle error args containing day or month or year correctly" in {
      val error  = FormError("startDate", "some.error", Seq("day"))
      val result = ViewUtils.errorHref(error, Nil)
      result shouldBe "startDate.day"
    }

    "handle single date field errors correctly" in {
      val error  = FormError("startDate", "error.date")
      val result = ViewUtils.errorHref(error, Nil)
      result shouldBe "startDate.day"
    }

    "handle single date field errors correctly with out yes no correctly" in {
      val error  = FormError("startDate", "error.date.invalid")
      val result = ViewUtils.errorHref(error, Nil)
      result shouldBe "startDate.day"
    }

    "handle general wildcard fallback error correctly" in {
      val error  = FormError("startDate", "some.other.error")
      val result = ViewUtils.errorHref(error, Nil)
      result shouldBe "startDate.day"
    }

  }

}
