import sbt.*

object AppDependencies {

  private lazy val mongoHmrcVersion = "1.3.0"
  val bootstrapVersion = "7.21.0"

  private val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"            % mongoHmrcVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-28"    % "8.5.0",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % "1.13.0-play-28",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28"    % bootstrapVersion
  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"      % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28"     % mongoHmrcVersion,
    "org.scalatest"          %% "scalatest"                   % "3.2.18",
    "org.scalatestplus"      %% "mockito-4-11"                % "3.2.18.0",
    "org.scalatestplus"      %% "scalacheck-1-17"             % "3.2.18.0",
    "wolfendale"             %% "scalacheck-gen-regexp"       % "0.1.2",
    "org.jsoup"              %  "jsoup"                       % "1.17.2",
    "org.mockito"            %% "mockito-scala-scalatest"     % "1.17.30",
    "org.wiremock"           % "wiremock-standalone"          % "3.4.2",
    "com.vladsch.flexmark"   % "flexmark-all"                 % "0.64.8"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
