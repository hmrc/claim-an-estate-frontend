import sbt._
import play.core.PlayVersion

object AppDependencies {

  private lazy val mongoHmrcVersion = "0.74.0"
  val bootstrapVersion = "7.20.0"

  private val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"            % mongoHmrcVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc"            % "7.16.0-play-28",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % "1.13.0-play-28",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28"    % bootstrapVersion
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"      % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28"     % mongoHmrcVersion,
    "org.scalatest"          %% "scalatest"                   % "3.2.16",
    "org.scalatestplus.play" %% "scalatestplus-play"          % "5.1.0",
    "org.scalatestplus"      %% "scalatestplus-scalacheck"    % "3.1.0.0-RC2",
    "wolfendale"             %% "scalacheck-gen-regexp"       % "0.1.2",
    "org.jsoup"              %  "jsoup"                       % "1.16.1",
    "com.typesafe.play"      %% "play-test"                   % PlayVersion.current,
    "org.scalatestplus"      %% "mockito-4-6"                 % "3.2.15.0",
    "com.github.tomakehurst" % "wiremock-standalone"          % "2.27.2",
    "com.vladsch.flexmark"   % "flexmark-all"                 % "0.64.8"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
