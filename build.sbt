import play.sbt.routes.RoutesKeys
import sbt.Def
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import scoverage.ScoverageKeys

lazy val appName: String = "claim-an-estate-frontend"

lazy val scoverageSettings = {

  val excludedPackages = Seq(
    "<empty>",
    ".*Reverse.*",
    ".*Routes.*",
    ".*standardError*.*",
    ".*main_template*.*",
    "uk.gov.hmrc.BuildInfo",
    "app.*",
    "prod.*",
    "config.*",
    "views.html.*",
    "testOnly.*",
    "com.kenshoo.play.metrics*.*",
    ".*LocalDateService.*",
    ".*LocalDateTimeService.*",
    ".*RichJsValue.*",
    ".*Repository.*",
    ".*testOnlyDoNotUseInAppConf.*",
    ".*AuditService.*",
    ".*models.Mode.*",
    ".*pages.Page.*"
  )
  Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimum := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}




lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(DefaultBuildSettings.scalaSettings: _*)
  .settings(DefaultBuildSettings.defaultSettings(): _*)
  .settings(SbtDistributablesPlugin.publishingSettings: _*)
  .settings(inConfig(Test)(testSettings): _*)
  .settings(majorVersion := 0)
  .settings(
    scalaVersion := "2.12.12",
    SilencerSettings(),
    name := appName,
    RoutesKeys.routesImport += "models._",
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
    scalacOptions ++= Seq("-feature"),
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    evictionWarningOptions in update :=
      EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    resolvers ++= Seq(
      Resolver.jcenterRepo
    ),
    // concatenate js
    Concat.groups := Seq(
      "javascripts/claimanestatefrontend-app.js" ->
        group(Seq(
          "javascripts/claimanestatefrontend.js",
          "javascripts/iebacklink.js",
          "javascripts/print.js"
        ))
    ),
    // prevent removal of unused code which generates warning errors due to use of third-party libs
    uglifyCompressOptions := Seq("unused=false", "dead_code=false"),
    pipelineStages := Seq(digest),
    // below line required to force asset pipeline to operate in dev rather than only prod
    pipelineStages in Assets := Seq(concat,uglify),
    // only compress files generated by concat
    includeFilter in uglify := GlobFilter("claimanestatefrontend-*.js")
  )
  .settings(scoverageSettings)

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  fork        := true,
  javaOptions ++= Seq(
    "-Dconfig.resource=test.application.conf"
  )
)

dependencyOverrides ++= AppDependencies.overrides

