import sbt._
import play.core.PlayVersion

object AppDependencies {

  private lazy val mongoHmrcVersion = "0.73.0"

  private val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"            % mongoHmrcVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc"            % "3.6.0-play-28",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % "1.9.0-play-28",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28"    % "5.25.0"
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28"     % mongoHmrcVersion,
    "org.scalatest"          %% "scalatest"                   % "3.2.13",
    "org.scalatestplus.play" %% "scalatestplus-play"          % "5.1.0",
    "org.scalatestplus"      %% "scalatestplus-scalacheck"    % "3.1.0.0-RC2",
    "wolfendale"             %% "scalacheck-gen-regexp"       % "0.1.2",
    "org.jsoup"              %  "jsoup"                       % "1.15.3",
    "com.typesafe.play"      %% "play-test"                   % PlayVersion.current,
    "org.scalatestplus"      %% "mockito-4-6"                 % "3.2.13.0",
    "com.github.tomakehurst" % "wiremock-standalone"          % "2.27.2",
    "com.vladsch.flexmark"   % "flexmark-all"                 % "0.62.2"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
