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

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import scala.language.implicitConversions

class PageSpec extends AnyFunSpec with Matchers {

  class TestPage(val title: String) extends Page {
    override def toString: String = title
  }

  describe("Page Companion Object") {

    it("should implicitly convert a Page instance into a String") {
      val page: Page = new TestPage("Settings")

      val text: String = page

      text shouldBe "Settings"
    }
  }

}
