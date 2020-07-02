#!/bin/bash

echo ""
echo "Applying migration IsAgentManagingEstate"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /isAgentManagingEstate                        controllers.IsAgentManagingEstateController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /isAgentManagingEstate                        controllers.IsAgentManagingEstateController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeIsAgentManagingEstate                  controllers.IsAgentManagingEstateController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeIsAgentManagingEstate                  controllers.IsAgentManagingEstateController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "isAgentManagingEstate.title = isAgentManagingEstate" >> ../conf/messages.en
echo "isAgentManagingEstate.heading = isAgentManagingEstate" >> ../conf/messages.en
echo "isAgentManagingEstate.checkYourAnswersLabel = isAgentManagingEstate" >> ../conf/messages.en
echo "isAgentManagingEstate.error.required = Select yes if isAgentManagingEstate" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryIsAgentManagingEstateUserAnswersEntry: Arbitrary[(IsAgentManagingEstatePage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[IsAgentManagingEstatePage.type]";\
    print "        value <- arbitrary[Boolean].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryIsAgentManagingEstatePage: Arbitrary[IsAgentManagingEstatePage.type] =";\
    print "    Arbitrary(IsAgentManagingEstatePage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(IsAgentManagingEstatePage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Adding helper method to CheckYourAnswersHelper"
awk '/class/ {\
     print;\
     print "";\
     print "  def isAgentManagingEstate: Option[AnswerRow] = userAnswers.get(IsAgentManagingEstatePage) map {";\
     print "    x =>";\
     print "      AnswerRow(";\
     print "        HtmlFormat.escape(messages(\"isAgentManagingEstate.checkYourAnswersLabel\")),";\
     print "        yesOrNo(x),";\
     print "        routes.IsAgentManagingEstateController.onPageLoad(CheckMode).url";\
     print "      )"
     print "  }";\
     next }1' ../app/utils/CheckYourAnswersHelper.scala > tmp && mv tmp ../app/utils/CheckYourAnswersHelper.scala

echo "Migration IsAgentManagingEstate completed"
