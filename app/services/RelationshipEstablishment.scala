/*
 * Copyright 2021 HM Revenue & Customs
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

package services

import config.FrontendAppConfig
import javax.inject.Inject
import play.api.Logging
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.Session

import scala.concurrent.{ExecutionContext, Future}

sealed trait RelationEstablishmentStatus

case object RelationshipFound extends RelationEstablishmentStatus
case object RelationshipNotFound extends RelationEstablishmentStatus
case class RelationshipError(reason : String) extends Exception(reason)

class RelationshipEstablishmentService @Inject()(
                                                  val authConnector: AuthConnector
                                                )(
                                                  implicit val config: FrontendAppConfig,
                                                  implicit val executionContext: ExecutionContext
                                                )
  extends RelationshipEstablishment with Logging {
  
  def check(internalId: String, utr: String)(implicit request: Request[AnyContent]): Future[RelationEstablishmentStatus] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    def failedRelationshipPF: PartialFunction[Throwable, Future[RelationEstablishmentStatus]] = {
      case FailedRelationship(msg) =>
        // relationship does not exist
        // $COVERAGE-OFF$
        logger.info(s"[Claiming][Session ID: ${Session.id(hc)}][UTR: $utr]" +
          s" Relationship does not exist in Estate IV for user $internalId due to $msg")
        // $COVERAGE-ON$
        Future.successful(RelationshipNotFound)
      case e : Throwable =>
        // $COVERAGE-OFF$
        logger.error(s"[Claiming][Session ID: ${Session.id(hc)}]" +
          s" Service was unable to determine if an IV relationship existed in Estates IV. Cannot continue with the journey")
        // $COVERAGE-ON$
        throw RelationshipError(e.getMessage)
    }

    authorised(Relationship(config.relationshipName, Set(BusinessKey(config.relationshipIdentifier, utr)))) {
      // $COVERAGE-OFF$
      logger.info(s"[Claiming][Session ID: ${Session.id(hc)}][UTR: $utr]" +
        s" Relationship established in Estate IV for user $internalId")
      // $COVERAGE-ON$
        Future.successful(RelationshipFound)
    } recoverWith {
      failedRelationshipPF
    }
  }

}

trait RelationshipEstablishment extends AuthorisedFunctions {

  def check(internalId: String, utr: String)(implicit request: Request[AnyContent]): Future[RelationEstablishmentStatus]

}
