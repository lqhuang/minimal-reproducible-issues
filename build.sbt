val scala3Version = "3.2.2"
val circeVersion = "0.14.4"

scalacOptions ++= Seq("-explain", "-Xcheck-macros")

lazy val root = project
  .in(file("."))
  .settings(
    name := "staging-with-case-class",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "org.scala-lang" %% "scala3-staging" % scala3Version,
      "org.scalameta" %% "munit" % "0.7.29" % Test
    )
  )
