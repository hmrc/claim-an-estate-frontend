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

package controllers

import controllers.actions.Actions
import handlers.ErrorHandler

import javax.inject.Inject
import models.{NormalMode, UserAnswers}
import pages.UTRPage
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{RelationshipEstablishment, RelationshipFound, RelationshipNotFound}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Session

import scala.concurrent.{ExecutionContext, Future}

class SaveUTRController @Inject() (
  actions: Actions,
  val controllerComponents: MessagesControllerComponents,
  sessionRepository: SessionRepository,
  relationship: RelationshipEstablishment,
  errorHandler: ErrorHandler
)(implicit ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport with Logging {

  def save(utr: String): Action[AnyContent] = actions.authWithSession.async { implicit request =>
    val utrPattern = """^\d{10}$""".r

    utr match {
      case utrPattern() =>

        relationship.check(request.internalId, utr) flatMap {
          case RelationshipFound =>
            logger.info(
              s"$logStart relationship is already established in IV for utr $utr sending user to successfully claimed"
            )

            Future.successful(Redirect(routes.IvSuccessController.onPageLoad))

          case RelationshipNotFound =>
            val userAnswers = request.userAnswers match {
              case Some(userAnswers) =>
                userAnswers.set(UTRPage, utr)
              case _                 =>
                UserAnswers(request.internalId).set(UTRPage, utr)
            }

            for {
              updatedAnswers <- Future.fromTry(userAnswers)
              _              <- sessionRepository.set(updatedAnswers)
            } yield {

              logger.info(s"$logStart user has started the claim an estate journey for utr $utr")

              Redirect(routes.IsAgentManagingEstateController.onPageLoad(NormalMode))
            }
        }

      case _ =>
        logger.error(s"$logStart Invalid UTR: $utr")
        errorHandler.internalServerErrorTemplate.map(html => InternalServerError(html))
    }

  }

  private def logStart(implicit hc: HeaderCarrier): String =
    s"[SaveUTRController][save][Session ID: ${Session.id(hc)}]"

}
