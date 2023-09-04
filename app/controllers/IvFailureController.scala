/*
 * Copyright 2023 HM Revenue & Customs
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

import config.FrontendAppConfig
import connectors.{EstatesStoreConnector, RelationshipEstablishmentConnector}
import controllers.actions.Actions

import javax.inject.Inject
import models.RelationshipEstablishmentStatus.{UnsupportedRelationshipStatus, UpstreamRelationshipError}
import models.{EstatesStoreRequest, RelationshipEstablishmentStatus}
import pages.{IsAgentManagingEstatePage, UTRPage}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AuditService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Session
import views.html.{EstateLocked, EstateNotFound, EstateStillProcessing}

import scala.concurrent.{ExecutionContext, Future}

class IvFailureController @Inject()(
                                     val controllerComponents: MessagesControllerComponents,
                                     appConfig: FrontendAppConfig,
                                     lockedView: EstateLocked,
                                     stillProcessingView: EstateStillProcessing,
                                     notFoundView: EstateNotFound,
                                     actions: Actions,
                                     relationshipEstablishmentConnector: RelationshipEstablishmentConnector,
                                     connector: EstatesStoreConnector,
                                     auditService: AuditService
                                   )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {
  private def renderFailureReason(utr: String, internalId: String, journeyId: String)(implicit hc : HeaderCarrier) = {
    relationshipEstablishmentConnector.journeyId(journeyId) map {
      case RelationshipEstablishmentStatus.Locked =>
        // $COVERAGE-OFF$
        logger.info(s"[Claiming][Estates IV][Session ID: ${Session.id(hc)}] $utr is locked")
        // $COVERAGE-ON$

        auditService.auditEstateClaimError(
          utr,
          internalId,
          s"User failed IV 3 times, has been locked out for 30 minutes, journey Id was $journeyId"
        )

        Redirect(routes.IvFailureController.estateLocked)
      case RelationshipEstablishmentStatus.NotFound =>
        // $COVERAGE-OFF$
        logger.info(s"[Claiming][Estates IV][Session ID: ${Session.id(hc)}] $utr was not found")
        // $COVERAGE-ON$
        Redirect(routes.IvFailureController.estateNotFound)
      case RelationshipEstablishmentStatus.InProcessing =>
        // $COVERAGE-OFF$
        logger.info(s"[Claiming][Estates IV][Session ID: ${Session.id(hc)}] $utr is processing")
        // $COVERAGE-ON$
        Redirect(routes.IvFailureController.estateStillProcessing)
      case UnsupportedRelationshipStatus(reason) =>
        // $COVERAGE-OFF$
        logger.warn(s"[Claiming][Estates IV][Session ID: ${Session.id(hc)}] Unsupported IV failure reason: $reason")
        // $COVERAGE-ON$
        Redirect(routes.FallbackFailureController.onPageLoad)
      case UpstreamRelationshipError(response) =>
        // $COVERAGE-OFF$
        logger.warn(s"[Claiming][Estates IV][Session ID: ${Session.id(hc)}] HTTP response: $response")
        // $COVERAGE-ON$
        Redirect(routes.FallbackFailureController.onPageLoad)
      case _ =>
        // $COVERAGE-OFF$
        logger.warn(s"[Claiming][Estates IV][Session ID: ${Session.id(hc)}] No errorKey in HTTP response")
        // $COVERAGE-ON$
        Redirect(routes.FallbackFailureController.onPageLoad)
    }
  }

  def onEstateIvFailure: Action[AnyContent] = actions.authWithData.async {
    implicit request =>

      request.userAnswers.get(UTRPage) match {
        case Some(utr) =>
          val queryString = request.getQueryString("journeyId")

          queryString.fold{
            // $COVERAGE-OFF$
            logger.warn(s"[Claiming][Estates IV][Session ID: ${Session.id(hc)}]" +
              s" unable to retrieve a journeyId to determine the reason")
            // $COVERAGE-ON$
            Future.successful(Redirect(routes.FallbackFailureController.onPageLoad))
          }{
            journeyId =>
              renderFailureReason(utr, request.internalId, journeyId)
          }
        case None =>
          // $COVERAGE-OFF$
          logger.warn(s"[Claiming][Estates IV][Session ID: ${Session.id(hc)}] unable to retrieve a UTR")
          // $COVERAGE-ON$
          Future.successful(Redirect(routes.FallbackFailureController.onPageLoad))
      }
  }

  def estateLocked : Action[AnyContent] = actions.authWithData.async {
    implicit request =>
      (for {
        utr <- request.userAnswers.get(UTRPage)
        isManagedByAgent <- request.userAnswers.get(IsAgentManagingEstatePage)
      } yield {
        connector.lock(EstatesStoreRequest(request.internalId, utr, isManagedByAgent, estateLocked = true)) map { _ =>
          // $COVERAGE-OFF$
          logger.info(s"[Claiming][Estates IV][Session ID: ${Session.id(hc)}]" +
            s" failed IV 3 times, estate is locked out from IV")
          // $COVERAGE-ON$
          Ok(lockedView(utr))
        }
      }) getOrElse {
        // $COVERAGE-OFF$
        logger.error(s"[Claiming][Estates IV][Session ID: ${Session.id(hc)}]" +
          s" unable to determine if estate is locked out from IV")
        // $COVERAGE-ON$
        Future.successful(Redirect(routes.SessionExpiredController.onPageLoad))
      }
  }

  def estateNotFound : Action[AnyContent] = actions.authWithData.async {
    implicit request =>
      request.userAnswers.get(UTRPage) map {
        utr =>
          // $COVERAGE-OFF$
          logger.info(s"[Claiming][Estates IV][Session ID: ${Session.id(hc)}]" +
            s" IV was unable to find the estate for utr $utr")
          // $COVERAGE-ON$
          Future.successful(Ok(notFoundView()))
      } getOrElse {
        // $COVERAGE-OFF$
        logger.error(s"[Claiming][Estates IV][Session ID: ${Session.id(hc)}]" +
          s" no utr stored in user answers when informing user the estate was not found")
        // $COVERAGE-ON$
        Future.successful(Redirect(routes.SessionExpiredController.onPageLoad))
      }
  }

  def estateNotFoundOnSubmit: Action[AnyContent] = Action { _ =>
    Redirect(appConfig.estatesRegistration)
  }

  def estateStillProcessing : Action[AnyContent] = actions.authWithData.async {
    implicit request =>
      request.userAnswers.get(UTRPage) map {
        utr =>
          // $COVERAGE-OFF$
          logger.info(s"[Claiming][Estates IV][Session ID: ${Session.id(hc)}]" +
            s" IV determined the estate utr $utr was still processing")
          // $COVERAGE-ON$
          Future.successful(Ok(stillProcessingView(utr)))
      } getOrElse {
        // $COVERAGE-OFF$
        logger.error(s"[Claiming][Estates IV][Session ID: ${Session.id(hc)}]" +
          s" no utr stored in user answers when informing user estate was still processing")
        // $COVERAGE-ON$
        Future.successful(Redirect(routes.SessionExpiredController.onPageLoad))
      }
  }
}
