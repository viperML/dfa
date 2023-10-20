val scala3Version = "3.3.1"

val http4sVersion = "1.0.0-M40"

lazy val root = project
  .in(file("."))
  .settings(
    name := "dfa",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    Compile / run / fork := true,
    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test,
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-client" % http4sVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "io.circe" %% "circe-generic" % "0.14.6",
    ),
    libraryDependencies += "org.typelevel" %% "cats-core" % "2.10.0",
    libraryDependencies += "org.typelevel" %% "cats-effect" % "3.5.2",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "log4cats-core" % "2.6.0", // Only if you want to Support Any Backend
      "org.typelevel" %% "log4cats-slf4j" % "2.6.0" // Direct Slf4j Support - Recommended
    ),
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.3.11" % Runtime,
    libraryDependencies += "co.fs2" %% "fs2-core" % "3.9.2",
    libraryDependencies += "dev.kovstas" %% "fs2-throttler" % "1.0.8"
  )
