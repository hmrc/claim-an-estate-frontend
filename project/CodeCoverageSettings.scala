import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {

  val excludedPackages: Seq[String] = Seq(
    "<empty>",
    ".*Routes.*",
    ".*testOnlyDoNotUseInAppConf.*"
  )

  private val settings: Seq[Setting[?]] = Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 87,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )

  def apply(): Seq[Setting[?]] = settings

}
