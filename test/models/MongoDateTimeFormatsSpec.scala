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

package models

import play.api.libs.json._

import java.time.LocalDateTime
import models.MongoDateTimeFormats.localDateTimeRead
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import java.time.format.DateTimeFormatter

class MongoDateTimeFormatsSpec extends AnyFreeSpec with Matchers with OptionValues {

  "localDateTimeRead" - {

    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    val expectedDate                 = LocalDateTime.parse("2020-05-18 14:10:30.000", formatter)

    "return a LocalDateTime when parsed a Json object containing" - {
      "a string date which contains a 'Z' character" in {
        val json                                = Json.obj(("$date", "2020-05-18T14:10:30.000Z"))
        val nameResult: JsResult[LocalDateTime] = json.validate[LocalDateTime]
        nameResult mustBe JsSuccess(expectedDate)
      }

      "a string date which without a 'Z' character" in {
        val json                                = Json.obj(("$date", "2020-05-18T14:10:30.000"))
        val nameResult: JsResult[LocalDateTime] = json.validate[LocalDateTime]
        nameResult mustBe JsSuccess(expectedDate)
      }

      "a nested Json Object which contains a number" in {
        val json                                = Json.obj(("$date", Json.obj(("$numberLong", "1589811030000"))))
        val nameResult: JsResult[LocalDateTime] = json.validate[LocalDateTime]
        nameResult mustBe JsSuccess(expectedDate)
      }

      "a number" in {
        val json                                = Json.obj(("$date", 1589811030000L))
        val nameResult: JsResult[LocalDateTime] = json.validate[LocalDateTime]
        nameResult mustBe JsSuccess(expectedDate)
      }
    }

    "throw a JsError when parsed a" - {
      "string without a 'Z'" in {
        val json                                = Json.obj(("$date", "NOT A DATE"))
        val nameResult: JsResult[LocalDateTime] = json.validate[LocalDateTime]
        nameResult mustBe JsError("Unexpected LocalDateTime Format")
      }

      "nested Json Object without a $numberLong field" in {
        val json                                = Json.obj(("$date", Json.obj(("$notNumberLong", 1589811030000L))))
        val nameResult: JsResult[LocalDateTime] = json.validate[LocalDateTime]
        nameResult mustBe JsError("Unexpected LocalDateTime Format")
      }

      "Json Object with no $date field" in {
        val json                                = Json.obj()
        val nameResult: JsResult[LocalDateTime] = json.validate[LocalDateTime]
        nameResult mustBe JsError("Unexpected LocalDateTime Format")
      }
    }
  }

}
