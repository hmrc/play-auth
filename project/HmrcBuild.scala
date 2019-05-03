import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys._
import sbt._
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import uk.gov.hmrc.{SbtArtifactory, SbtAutoBuildPlugin}

object HmrcBuild extends Build {

  val appName = "play-auth"

  lazy val scoverageSettings: Seq[Def.Setting[_ >: String with Double with Boolean]] = {
    import scoverage._

    Seq(
      ScoverageKeys.coverageExcludedPackages :=
        """<empty>;
          |.*BuildInfo.*;
          |.*json.*;""".stripMargin,

      ScoverageKeys.coverageMinimum := 80,
      ScoverageKeys.coverageFailOnMinimum := false,
      ScoverageKeys.coverageHighlighting := true
    )
  }

  lazy val library = Project(appName, file("."))
    .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
    .settings(
      name := appName,
      scalaVersion := "2.11.7",
      crossScalaVersions := Seq("2.11.7"),
      libraryDependencies ++= BuildDependencies(),
      Developers()
    )
    .settings(scoverageSettings: _*)
    .settings(majorVersion := 2)
    .settings(resolvers += Resolver.bintrayRepo("hmrc", "releases"),
      resolvers += "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/"
    )
}

private object BuildDependencies {

  val compile: Seq[ModuleID] = Seq(
    ws % "provided",
    "com.typesafe.play" %% "play" % PlayVersion.current,
    "uk.gov.hmrc" %% "http-verbs" % "9.7.0-play-25",
    "net.ceedubs" %% "ficus" % "1.1.1"
  )

  val testScope = "test"
  val test: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % "3.0.4" % testScope,
    "org.pegdown" % "pegdown" % "1.6.0" % testScope,
    "org.mockito" % "mockito-core" % "2.8.47" % testScope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % testScope
  )

  def apply(): Seq[ModuleID] = compile ++ test

}

object Developers {
  def apply(): Def.Setting[List[Developer]] = developers := List[Developer]()
}
