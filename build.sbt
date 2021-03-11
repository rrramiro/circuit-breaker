name := "circuit-breaker"

version := "0.0.1"

ThisBuild / scalaVersion := "2.13.3"

ThisBuild / scalafmtOnCompile := true

ThisBuild / scalacOptions ++= Seq(
  "-encoding",
  "UTF-8",
  "-Xfatal-warnings",
  "-deprecation",
  "-feature",
  "-Yrangepos"
)

ThisBuild / libraryDependencies ++= Seq(
  "io.monix" %% "monix" % "3.2.2",
  "io.monix" %% "monix-catnap" % "3.0.0",
  "org.typelevel" %% "cats-effect" % "2.0.0",
  "io.monix" %% "minitest-laws" % "2.9.3" % Test
)

ThisBuild / testFrameworks := Seq(new TestFramework("minitest.runner.Framework"))

Global / onChangedBuildSource := ReloadOnSourceChanges
