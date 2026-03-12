import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {

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

  private val settings: Seq[Setting[?]] = Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 88,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )

  def apply(): Seq[Setting[?]] = settings

}
