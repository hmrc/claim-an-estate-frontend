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

package config

import com.google.inject.{Inject, Singleton}
import controllers.routes
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.Call
import uk.gov.hmrc.hmrcfrontend.config.ContactFrontendConfig

@Singleton
class FrontendAppConfig @Inject() (val configuration: Configuration,
                                   contactFrontendConfig: ContactFrontendConfig) {

  lazy val serviceName: String = configuration.get[String]("serviceName")

  val betaFeedbackUrl = s"${contactFrontendConfig.baseUrl.get}/contact/beta-feedback?service=${contactFrontendConfig.serviceId.get}"

  lazy val estatesRegistration: String = configuration.get[String]("urls.estatesRegistration")
  lazy val authUrl: String = configuration.get[Service]("auth").baseUrl
  lazy val loginUrl: String = configuration.get[String]("urls.login")
  lazy val loginContinueUrl: String = configuration.get[String]("urls.loginContinue")
  lazy val logoutUrl: String = configuration.get[String]("urls.logout")

  lazy val logoutAudit: Boolean =
    configuration.get[Boolean]("microservice.services.features.auditing.logout")

  lazy val countdownLength: Int = configuration.get[Int]("timeout.countdown")
  lazy val timeoutLength: Int = configuration.get[Int]("timeout.length")

  lazy val estatesContinueUrl: String = {
    configuration.get[String]("urls.maintainContinue")
  }

  lazy val taxEnrolmentsUrl: String = configuration.get[Service]("microservice.services.tax-enrolments").baseUrl + "/tax-enrolments"

  lazy val relationshipName : String =
    configuration.get[String]("microservice.services.self.relationship-establishment.name")

  lazy val relationshipIdentifier : String =
    configuration.get[String]("microservice.services.self.relationship-establishment.identifier")

  lazy val estatesStoreUrl: String = configuration.get[Service]("microservice.services.estates-store").baseUrl + "/estates-store"

  lazy val relationshipEstablishmentUrl : String =
    configuration.get[Service]("microservice.services.relationship-establishment").baseUrl


  lazy val relationshipEstablishmentStubbed: Boolean =
    configuration.get[Boolean]("microservice.services.features.stubRelationshipEstablishment")

  def relationshipEstablishmentFrontendtUrl(utr: String) : String = {
    if (relationshipEstablishmentStubbed) {
      s"${configuration.get[String]("urls.testOnly.estatesIV")}/$utr"
    } else {
      s"${configuration.get[String]("urls.estatesIV")}/$utr"
    }
  }

  lazy val successUrl : String =
    configuration.get[String]("urls.successUrl")

  lazy val failureUrl : String =
    configuration.get[String]("urls.failureUrl")

  lazy val languageTranslationEnabled: Boolean =
    configuration.get[Boolean]("microservice.services.features.welsh-translation")

  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  def routeToSwitchLanguage: String => Call =
    (lang: String) => routes.LanguageSwitchController.switchToLanguage(lang)

}
