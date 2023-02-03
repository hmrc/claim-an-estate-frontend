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

package repositories

import com.google.inject.Singleton
import config.MongoConfig
import models.UserAnswers
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DefaultSessionRepository @Inject()(
                                          val mongoComponent: MongoComponent,
                                          val config: MongoConfig
                                        )(implicit val ec: ExecutionContext)
  extends PlayMongoRepository[UserAnswers](
    collectionName = "user-answers",
    mongoComponent = mongoComponent,
    domainFormat = UserAnswers.format,
    indexes = Seq(
      IndexModel(
        ascending("lastUpdated"),
        IndexOptions()
          .unique(false)
          .name("user-answers-last-updated-index")
          .expireAfter(config.ttlInSeconds, TimeUnit.SECONDS))
    ),
    replaceIndexes = config.dropIndexes
  )
    with SessionRepository {

  private def eqId(id: String) = Filters.eq("_id", id)

  override def get(id: String): Future[Option[UserAnswers]] =
    collection.find(eqId(id)).headOption()

  override def set(userAnswers: UserAnswers): Future[Boolean] =
    collection.replaceOne(eqId(userAnswers.id), userAnswers, ReplaceOptions().upsert(true))
      .head()
      .map(_.wasAcknowledged())
}
