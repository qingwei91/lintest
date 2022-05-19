ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.1"

lazy val root = (project in file("."))
  .settings(
    name := "lintest",
    libraryDependencies += "com.disneystreaming" %% "weaver-cats" % "0.7.11" % Test,
    libraryDependencies += "com.softwaremill.diffx" %% "diffx-core" % "0.7.0" % Test,
    testFrameworks += new TestFramework("weaver.framework.CatsEffect")
  )
