import scoverage.ScoverageKeys

ThisBuild / scalaVersion := "2.13.14"
ThisBuild / majorVersion := 0

lazy val appName: String = "claim-an-estate-frontend"

lazy val scoverageSettings = {

  val excludedPackages: Seq[String] = Seq(
    "<empty>",
    ".*Routes.*",
    "config.*",
    "views.html.*",
    ".*testOnlyDoNotUseInAppConf.*",
    ".*AuditService.*",
    ".*models.Mode.*",
    ".*pages.Page.*",
    ".*utils.DateErrorFormatter",
    ".*models.RichJsObject.*",
    ".*models.RichJsValue.*"
  )

  Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 88,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    Test / javaOptions ++= Seq(
      "-Dconfig.resource=test.application.conf"
    ),
    routesImport += "models._",
    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "play.twirl.api.HtmlFormat._",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "views.ViewUtils._",
      "models.Mode",
      "controllers.routes._"
    ),
    PlayKeys.playDefaultPort := 8830,
    scalacOptions ++= Seq(
      "-feature",
      "-Wconf:src=routes/.*:s",
      "-Wconf:cat=unused-imports&src=html/.*:s"
    ),
    libraryDependencies ++= AppDependencies(),
    // concatenate js
    Concat.groups := Seq(
      "javascripts/claimanestatefrontend-app.js" ->
        group(Seq(
          "javascripts/claimanestatefrontend.js",
          "javascripts/iebacklink.js"
        ))
    ),
    // prevent removal of unused code which generates warning errors due to use of third-party libs
    uglifyCompressOptions := Seq("unused=false", "dead_code=false"),
    pipelineStages := Seq(digest),
    // below line required to force asset pipeline to operate in dev rather than only prod
    Assets / pipelineStages := Seq(concat,uglify),
    // only compress files generated by concat
    uglify / includeFilter := GlobFilter("claimanestatefrontend-*.js")
  )
  .settings(scoverageSettings)

addCommandAlias("scalastyleAll", "all scalastyle Test/scalastyle")
