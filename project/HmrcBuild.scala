import sbt.Keys._
import sbt._
import uk.gov.hmrc.{SbtArtifactory, SbtAutoBuildPlugin}
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

object HmrcBuild extends Build {

  val appName = "play-auth"

  lazy val library = Project(appName, file("."))
    .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
    .settings(
      name := appName,
      scalaVersion := "2.11.7",
      crossScalaVersions := Seq("2.11.7"),
      libraryDependencies ++= BuildDependencies(),
      Developers()
    )
    .settings(majorVersion := 2)
    .settings(resolvers += Resolver.bintrayRepo("hmrc", "releases"),
      resolvers += "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/"
    )
}

private object BuildDependencies {

  import play.core.PlayVersion
  import play.sbt.PlayImport._

  val compile = Seq(
    "com.typesafe.play" %% "play" % PlayVersion.current % "provided",
    ws % "provided",
    "uk.gov.hmrc" %% "http-verbs" % "6.3.0" % "provided",
    "net.ceedubs" %% "ficus" % "1.1.1"
  )

  val testScope = "test"
  val test = Seq(
    "org.scalatest" %% "scalatest" % "2.2.6" % testScope,
    "org.pegdown" % "pegdown" % "1.5.0" % testScope,
    "org.mockito" % "mockito-core" % "2.8.47" % testScope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % testScope
  )

  def apply() = compile ++ test

}

object Developers {

  def apply() = developers := List[Developer]()
}
