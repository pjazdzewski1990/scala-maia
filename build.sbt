/* Any copyright is dedicated to the Public Domain.
 * http://creativecommons.org/publicdomain/zero/1.0/ */

name := "maia"
version := "1.0"
scalaVersion := "2.12.1"
scalaOrganization := "org.typelevel"
scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-unchecked",
  "-deprecation",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  // Typelevel flags...
  "-Yinduction-heuristics",
  "-Xlint:strict-unsealed-patmat",
  "-Xexperimental"
)
licenses := Seq("MPLv2" -> url("http://mozilla.org/MPL/2.0/"))
homepage := Some(url("http://github.com/tel/scala-maia"))
resolvers += Resolver.sonatypeRepo("releases")

val catsVersion = "0.9.0"
val circeVersion = "0.7.0"
val shapelessVersion = "2.3.2"
val uTestVersion = "0.4.5"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.typelevel" %% "cats" % catsVersion,
  "com.chuusai" %% "shapeless" % shapelessVersion,
  "com.lihaoyi" %% "utest" % uTestVersion % "test"
)

testFrameworks +=
  new TestFramework("utest.runner.Framework")

wartremoverErrors ++= Warts.unsafe

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")
