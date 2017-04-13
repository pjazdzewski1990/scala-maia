/* Any copyright is dedicated to the Public Domain.
 * http://creativecommons.org/publicdomain/zero/1.0/ */

lazy val commonSettings = Seq(
  version := "1.0",
  organization := "com.jspha",
  scalaVersion := "2.12.1",
  scalacOptions ++= Seq(
    "-target:jvm-1.8",
    "-unchecked",
    "-deprecation",
    "-Yno-adapted-args",
    "-Ywarn-numeric-widen"
  ),
  licenses := Seq("MPLv2" -> url("http://mozilla.org/MPL/2.0/")),
  homepage := Some(url("http://github.com/MaiaOrg/scala-maia")),
  resolvers += Resolver.sonatypeRepo("releases"),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3"),
  wartremoverErrors ++= Warts.allBut(
    Wart.Any,
    Wart.AsInstanceOf,
    Wart.ExplicitImplicitTypes,
    Wart.Nothing
  ),
  libraryDependencies ++= commonDependencies
)

val catsVersion = "0.9.0"
val circeVersion = "0.7.0"
val shapelessVersion = "2.3.2"
val uTestVersion = "0.4.5"

lazy val commonDependencies = Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.typelevel" %% "cats" % catsVersion,
  "com.chuusai" %% "shapeless" % shapelessVersion
)

lazy val maia = project
  .in(file("maia"))
  .settings(commonSettings: _*)
  .settings(
    name := "maia",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "utest" % uTestVersion % "test"
    ),
    testFrameworks +=
      new TestFramework("utest.runner.Framework")
  )

// The following lines enable automatic ScalaStyle linting during tests
lazy val testScalastyle = taskKey[Unit]("testScalastyle")
testScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle
  .in(Test)
  .toTask("")
  .value
(test in Test) := ((test in Test) dependsOn testScalastyle).value
