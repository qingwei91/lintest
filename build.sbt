ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.1"

lazy val root = (project in file("."))
  .settings(
    name := "lintest",
    libraryDependencies ++= Seq(
      "org.typelevel"          %% "cats-effect" % "3.3.12",
      "org.typelevel"          %% "cats-parse"  % "0.3.6",
      "com.disneystreaming"    %% "weaver-cats" % "0.7.11" % Test,
      "com.softwaremill.diffx" %% "diffx-core"  % "0.7.0"  % Test
    ),
    testFrameworks += new TestFramework("weaver.framework.CatsEffect")
  )
