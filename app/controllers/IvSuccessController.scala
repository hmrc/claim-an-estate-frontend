/*
 * Copyright 2026 HM Revenue Copyright 2024 HM Revenue & Customs Customs
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
import connectors.TaxEnrolmentsConnector
import controllers.actions.{Actions, AuthPartialFunctions}
import handlers.ErrorHandler
import models.requests.OptionalDataRequest
import models.{NormalMode, TaxEnrolmentRequest, UserAnswers}
import pages.{HasEnrolled, IsAgentManagingEstatePage, UTRPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{AuditService, RelationshipEstablishment, RelationshipFound, RelationshipNotFound}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Session
import views.html.IvSuccessView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IvSuccessController @Inject()(
                                     override val messagesApi: MessagesApi,
                                     actions: Actions,
                                     val controllerComponents: MessagesControllerComponents,
                                     relationshipEstablishment: RelationshipEstablishment,
                                     taxEnrolmentsConnector: TaxEnrolmentsConnector,
                                     view: IvSuccessView,
                                     errorHandler: ErrorHandler,
                                     auditService: AuditService,
                                     sessionRepository: SessionRepository
                                   )(implicit ec: ExecutionContext, val config: FrontendAppConfig)
  extends FrontendBaseController with I18nSupport with AuthPartialFunctions with Logging {

  def onPageLoad(): Action[AnyContent] = actions.authWithSession.async {
    implicit request =>
      request.userAnswers match {
        case Some(userAnswers) =>
          userAnswers.get(UTRPage).map { utr =>

            lazy val onRelationshipNotFound = Future.successful(Redirect(routes.IsAgentManagingEstateController.onPageLoad(NormalMode)))

            relationshipEstablishment.check(request.internalId, utr) flatMap {
              case RelationshipFound => onRelationshipFound(utr, userAnswers)
              case RelationshipNotFound =>
                logger.warn(s"[Claiming][Session ID: ${Session.id(hc)}]" +
                  s" no relationship found in Estates IV, cannot continue with enrolling the credential," +
                  s" sending the user back to the start of Estates IV")
                onRelationshipNotFound
            }
          } getOrElse {
            logger.warn(s"[Claiming][Session ID: ${Session.id(hc)}]" +
              s" no utr found in user answers, unable to continue with enrolling credential and claiming the estate on behalf of the user")
            noUtrOnSuccess(request)
          }

        case None =>
          logger.warn(s"[Claiming][Session ID: ${Session.id(hc)}]" +
            s" no user answers found, unable to continue with enrolling credential and claiming the estate on behalf of the user")
          noUtrOnSuccess(request)
      }
  }

  private def onRelationshipFound(utr: String, userAnswers: UserAnswers)(implicit request: OptionalDataRequest[_]): Future[Result] = {

    val hasEnrolled: Boolean = userAnswers.get(HasEnrolled).getOrElse(false)

    if (hasEnrolled) {
      auditService.auditEstateClaimed(utr, request.internalId)
      val isAgentManagingEstate = userAnswers.get(IsAgentManagingEstatePage).getOrElse(false)
      Future.successful(Ok(view(isAgentManagingEstate, utr)))
    } else {
      (for {
        _ <- taxEnrolmentsConnector.enrol(TaxEnrolmentRequest(utr))
        ua <- Future.fromTry(userAnswers.set(HasEnrolled, true))
        _ <- sessionRepository.set(ua)
      } yield {
        auditService.auditEstateClaimed(utr, request.internalId)
        val isAgentManagingEstate = userAnswers.get(IsAgentManagingEstatePage).getOrElse(false)
        logger.info(s"[Claiming][Session ID: ${Session.id(hc)}]" +
            s" successfully enrolled utr $utr to users credential after passing Estates IV, user can now maintain the estate")
        Ok(view(isAgentManagingEstate, utr))
      }) recoverWith {
        case e =>
          Future.fromTry(userAnswers.set(HasEnrolled, false)).flatMap { ua =>
            sessionRepository.set(ua).flatMap { _ =>
              logger.error(s"[Claiming][Session ID: ${Session.id(hc)}]" +
                s" failed to create enrolment for utr $utr with tax-enrolments," +
                s" users credential has not been updated, user needs to claim again")
              auditService.auditEstateClaimError(utr, request.internalId, e.getMessage)
              errorHandler.internalServerErrorTemplate.map(html => InternalServerError(html))
            }
          }
      }
    }
  }

  private def noUtrOnSuccess(request: OptionalDataRequest[AnyContent])(implicit hc: HeaderCarrier) = {
    auditService.auditEstateClaimError("Unknown", request.internalId, "No UTR available on success")
    Future.successful(Redirect(routes.SessionExpiredController.onPageLoad))
  }

  def onSubmit(): Action[AnyContent] = actions.authWithSession { _ =>
      Redirect(config.estatesContinueUrl)
  }
}
