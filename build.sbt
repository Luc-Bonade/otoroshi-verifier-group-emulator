import Dependencies._

ThisBuild / scalaVersion     := "2.12.13"
ThisBuild / version          := "1.0.0-dev"
ThisBuild / organization     := "fr.maif.otoroshi.plugins"
ThisBuild / organizationName := "MAIF"

lazy val root = (project in file("."))
  .settings(
    name := "otoroshi-verifier-group-emulator-plugin",
    fork := true,
    libraryDependencies ++= Seq(
      "fr.maif" %% "otoroshi" % "1.5.0-alpha.14",
      scalaTest % Test
    )
  )