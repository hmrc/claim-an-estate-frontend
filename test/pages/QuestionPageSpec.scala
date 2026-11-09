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

package pages

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.JsPath
import queries.{Gettable, Settable}

class QuestionPageSpec extends AnyWordSpec with Matchers {

  // Create a concrete dummy implementation to test the trait properties
  case object TestBooleanPage extends QuestionPage[Boolean] {
    override def path: JsPath = JsPath \ "test"
  }

  "A QuestionPage" should {

    "implement Page" in {
      TestBooleanPage shouldBe a[Page]
    }

    "implement Gettable with the correct type argument" in {
      TestBooleanPage shouldBe a[Gettable[?]]
    }

    "implement Settable with the correct type argument" in {
      TestBooleanPage shouldBe a[Settable[?]]
    }
  }

}
