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
import connectors.EstatesStoreConnector
import controllers.actions._
import javax.inject.Inject
import models.EstatesStoreRequest
import pages.{IsAgentManagingEstatePage, UTRPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{RelationshipEstablishment, RelationshipFound, RelationshipNotFound}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Session
import views.html.BeforeYouContinueView

import scala.concurrent.{ExecutionContext, Future}

class BeforeYouContinueController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       relationship: RelationshipEstablishment,
                                       actions: Actions,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: BeforeYouContinueView,
                                       connector: EstatesStoreConnector
                                     )(implicit ec: ExecutionContext,
                                       config: FrontendAppConfig) extends FrontendBaseController with I18nSupport with AuthPartialFunctions with Logging {

  def onPageLoad: Action[AnyContent] = actions.authWithData.async {
    implicit request =>

      request.userAnswers.get(UTRPage) map { utr =>
        def body = {
            Future.successful(Ok(view(utr)))
        }
        relationship.check(request.internalId, utr) flatMap {
          case RelationshipFound =>
            logger.info(s"[Claiming][Session ID: ${Session.id(hc)}]" +
              s"relationship is already established in IV for utr $utr sending user to successfully claimed")
            Future.successful(Redirect(routes.IvSuccessController.onPageLoad()))
          case RelationshipNotFound =>
            body
        }
      } getOrElse {
        logger.error(s"[Claiming][Session ID: ${Session.id(hc)}]" +
          s" no utr available in user answers, cannot continue with claiming the estate")
        Future.successful(Redirect(routes.SessionExpiredController.onPageLoad()))
      }
  }

  def onSubmit: Action[AnyContent] = actions.authWithData.async {
    implicit request =>

      (for {
        utr <- request.userAnswers.get(UTRPage)
        isManagedByAgent <- request.userAnswers.get(IsAgentManagingEstatePage)
      } yield {

        def onRelationshipNotFound =  {

          val successRedirect = config.successUrl
          val failureRedirect = config.failureUrl

          val host = config.relationshipEstablishmentFrontendtUrl(utr)

          val queryString: Map[String, Seq[String]] = Map(
            "successUrl" -> Seq(successRedirect),
            "failureUrl" -> Seq(failureRedirect)
          )

          connector.lock(EstatesStoreRequest(request.internalId, utr, isManagedByAgent, estateLocked = false)) map { _ =>
            logger.info(s"[Claiming][Session ID: ${Session.id(hc)}] " +
              s"saved users utr $utr in estates-store so they can be identified when they return from Estates IV. " +
              s"Sending the user into Estates IV to answer questions")
            Redirect(host, queryString)
          }

        }

        relationship.check(request.internalId, utr) flatMap {
          case RelationshipFound =>
            logger.info(s"[Claiming][Session ID: ${Session.id(hc)}]" +
              s" relationship is already established in IV for utr $utr sending user to successfully claimed")
            Future.successful(Redirect(routes.IvSuccessController.onPageLoad()))
          case RelationshipNotFound =>
            onRelationshipNotFound
        }
      }) getOrElse {
        logger.error(s"[Claiming][Session ID: ${Session.id(hc)}]" +
          s" no utr available in user answers, cannot continue with claiming the estate")
        Future.successful(Redirect(routes.SessionExpiredController.onPageLoad()))
      }
  }
}
