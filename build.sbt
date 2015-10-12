name := """wadhamball"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "1.0.1",
  "com.typesafe.play" %% "play-slick-evolutions" % "1.0.1",
  "com.h2database" % "h2" % "1.4.189",
  "org.postgresql" % "postgresql" % "9.4-1204-jdbc42"
)

PlayKeys.playOmnidoc := false

routesGenerator := InjectedRoutesGenerator

scalacOptions in Test ++= Seq("-Yrangepos")

fork in Test := false

lazy val root = (project in file(".")).enablePlugins(PlayScala)

// LESS files starting with an underscore are partial less files and should not be compiles
// (They are imported by other stylesheets)
includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"