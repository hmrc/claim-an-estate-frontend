/*
 * Copyright 2020 HM Revenue & Customs
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
import controllers.actions._
import handlers.ErrorHandler
import javax.inject.Inject
import models.requests.OptionalDataRequest
import models.{EnrolmentCreated, EnrolmentFailed, NormalMode, TaxEnrolmentRequest}
import pages.{IsAgentManagingEstatePage, UTRPage}
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditService, RelationshipEstablishment, RelationshipFound, RelationshipNotFound}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import utils.Session
import views.html.IvSuccessView

import scala.concurrent.{ExecutionContext, Future}

class IvSuccessController @Inject()(
                                     override val messagesApi: MessagesApi,
                                     actions: Actions,
                                     val controllerComponents: MessagesControllerComponents,
                                     relationshipEstablishment: RelationshipEstablishment,
                                     taxEnrolmentsConnector: TaxEnrolmentsConnector,
                                     view: IvSuccessView,
                                     errorHandler: ErrorHandler,
                                     auditService: AuditService
                                   )(implicit ec: ExecutionContext, val config: FrontendAppConfig)
  extends FrontendBaseController with I18nSupport with AuthPartialFunctions {

  private val logger: Logger = Logger(getClass)

  def onPageLoad(): Action[AnyContent] = actions.authWithSession.async {
    implicit request: OptionalDataRequest[AnyContent] =>
      request.userAnswers match {
        case Some(userAnswers) =>
          userAnswers.get(UTRPage).map { utr =>

            def onRelationshipFound = {
              taxEnrolmentsConnector.enrol(TaxEnrolmentRequest(utr)) map {

                case EnrolmentCreated =>

                  auditService.auditEstateClaimed(utr, request.internalId)

                  val isAgentManagingEstate = userAnswers.get(IsAgentManagingEstatePage) match {
                    case None => false
                    case Some(value) => value
                  }
                  logger.info(s"[Claiming][Session ID: ${Session.id(hc)}]" +
                    s" successfully enrolled utr $utr to users credential after passing Estates IV, user can now maintain the estate")
                  Ok(view(isAgentManagingEstate, utr))

                case response: EnrolmentFailed =>
                  auditService.auditEstateClaimFailed(utr, request.internalId, response)
                  InternalServerError(errorHandler.internalServerErrorTemplate)

              } recover {
                case e =>
                  logger.error(s"[Claiming][Session ID: ${Session.id(hc)}]" +
                    s" failed to create enrolment for utr $utr with tax-enrolments," +
                    s" users credential has not been updated, user needs to claim again")
                  auditService.auditEstateClaimError(utr, request.internalId, e.getMessage)
                  InternalServerError(errorHandler.internalServerErrorTemplate)
              }
            }

            lazy val onRelationshipNotFound = Future.successful(Redirect(routes.IsAgentManagingEstateController.onPageLoad(NormalMode)))

            relationshipEstablishment.check(request.internalId, utr) flatMap {
              case RelationshipFound => onRelationshipFound
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

  private def noUtrOnSuccess(request: OptionalDataRequest[AnyContent])(implicit hc: HeaderCarrier) = {
    auditService.auditEstateClaimError("Unknown", request.internalId, "No UTR available on success")
    Future.successful(Redirect(routes.SessionExpiredController.onPageLoad()))
  }
}
