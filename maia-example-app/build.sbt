lazy val commonSettings = Seq(
  organization := "com.jspha",
  scalaVersion := "2.12.1",
  version := "1.0",
  resolvers += Resolver.sonatypeRepo("snapshots"),
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
  )
)

val catsVersion = "0.9.0"
val circeVersion = "0.7.0"
val http4sVersion = "0.17.0-SNAPSHOT"
val fs2Version = "0.9.4"

lazy val maia = ProjectRef(file(".."), "maia")

lazy val server = project
  .in(file("server"))
  .settings(commonSettings: _*)
  .dependsOn(maia)
  .dependsOn(sharedJvm)
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl" % http4sVersion
    )
  )
  .settings( // bring ui compiled artifacts in to resources
    (resources in Compile) <+= Def.task {
      (artifactPath in (ui, Compile, fullOptJS)).value
    } dependsOn (fullOptJS in (ui, Compile)),
    (resources in Compile) <+= Def.task {
      (artifactPath in (ui, Compile, packageMinifiedJSDependencies)).value
    } dependsOn (fullOptJS in (ui, Compile))
  )

lazy val shared = crossProject
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .settings(commonSettings: _*)

lazy val sharedJvm = shared.jvm.settings(name := "sharedJvm")
lazy val sharedJs = shared.js.settings(name := "sharedJs")

val reactJsVersion = "15.0.2"

lazy val ui = project
  .in(file("ui"))
  .settings(commonSettings: _*)
  .dependsOn(sharedJs)
  .enablePlugins(ScalaJSPlugin)
  .settings(
    skip in packageJSDependencies := false,
    libraryDependencies ++= Seq(
      )
  )
  .settings(workbenchSettings)
  .settings(
    bootSnippet := "jspha.qubit.ui.Runtime().main();",
    refreshBrowsers <<= refreshBrowsers.triggeredBy(fastOptJS in Compile)
  )
