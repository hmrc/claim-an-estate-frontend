/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.testOnlyDoNotUseInAppConf

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.actions.IdentifierAction
import play.api.Logging
import play.api.i18n.MessagesApi
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Session

import scala.concurrent.{ExecutionContext, Future}

case class BusinessKey(name: String,value: String)

object BusinessKey {
  implicit val format: OFormat[BusinessKey] = Json.format[BusinessKey]
}

case class Relationship(relationshipName: String, businessKeys: Set[BusinessKey], credId: String)

object Relationship {
  implicit val format: OFormat[Relationship] = Json.format[Relationship]
}

case class RelationshipJson(relationship: Relationship, ttlSeconds: Int = 1440)

object RelationshipJson {
  implicit val format: OFormat[RelationshipJson] = Json.format[RelationshipJson]
}

class RelationshipEstablishmentConnector @Inject()(val httpClient: HttpClientV2, config: FrontendAppConfig)
                                                  (implicit val ec : ExecutionContext) {

  private val relationshipEstablishmentPostUrl: String = s"${config.relationshipEstablishmentUrl}/relationship-establishment/relationship/"

  private def relationshipEstablishmentGetUrl(credId :String): String =
    s"${config.relationshipEstablishmentUrl}/relationship-establishment/relationship/$credId"

  private def relationshipEstablishmentDeleteUrl(credId: String): String =
    s"${config.relationshipEstablishmentUrl}/test/relationship/$credId"

  private def newRelationship(credId: String, utr: String): Relationship =
    Relationship(config.relationshipName, Set(BusinessKey(config.relationshipIdentifier, utr)), credId)

  def createRelationship(credId: String, utr: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] =
    httpClient
      .post(url"$relationshipEstablishmentPostUrl")
      .withBody(Json.toJson(RelationshipJson(newRelationship(credId, utr))))
      .execute[HttpResponse]

  def getRelationship(credId: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] =
    httpClient
      .get(url"${relationshipEstablishmentGetUrl(credId)}")
      .execute[HttpResponse]

  def deleteRelationship(credId: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] =
    httpClient
      .delete(url"${relationshipEstablishmentDeleteUrl(credId)}")
      .execute[HttpResponse]
}

/**
 * Test controller and connector to relationship-establishment to set a relationship for a given UTR.
 * This will then enable the service to "succeed" and "fail" an IV check without having to go into EstateIV.
 */
class TestRelationshipEstablishmentController @Inject()(
                                                         override val messagesApi: MessagesApi,
                                                         val controllerComponents: MessagesControllerComponents,
                                                         relationshipEstablishmentConnector: RelationshipEstablishmentConnector,
                                                         identify: IdentifierAction
                                                       )
                                                       (implicit ec : ExecutionContext)
  extends FrontendBaseController with Logging {

  def check(utr: String): Action[AnyContent] = identify.async {
    implicit request =>

      logger.warn(s"[Session ID: ${Session.id(hc)}][UTR: $utr]" +
        s" EstateIV is using a test route, you don't want this in production.")

      val succeedRegex = "(2\\d{9})".r
      val failRegex = "(4\\d{9})".r

      def insertRelationship(): Future[Result] = relationshipEstablishmentConnector
        .createRelationship(request.credentials.providerId, utr) map {
        _ =>
          Redirect(controllers.routes.IvSuccessController.onPageLoad)
      }

      utr match {
        case "5000000001" => insertRelationship()
        case succeedRegex(_) => insertRelationship()
        case failRegex(_) =>
          Future.successful(Redirect(controllers.routes.IvFailureController.onEstateIvFailure))
        case _ =>
          Future.successful(Redirect(controllers.routes.IvFailureController.onEstateIvFailure))
      }
  }

}
