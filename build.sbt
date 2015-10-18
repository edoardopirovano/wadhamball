name := """wadhamball"""

version := "1.0"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "1.0.1",
  "com.typesafe.play" %% "play-slick-evolutions" % "1.0.1",
  "com.h2database" % "h2" % "1.4.189",
  "org.postgresql" % "postgresql" % "9.4-1204-jdbc42",
  ws
)

PlayKeys.playOmnidoc := false

routesGenerator := InjectedRoutesGenerator

scalacOptions in Test ++= Seq("-Yrangepos")

fork in Test := false

lazy val root = (project in file(".")).enablePlugins(PlayScala, JavaServerAppPackaging)

// LESS files starting with an underscore are partial less files and should not be compiles
// (They are imported by other stylesheets)
includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"

javaOptions in Universal ++= Seq(
  "-J-Xmx6g",
  "-J-Xms1g",
  // Since play uses separate pidfile we have to provide it with a proper path
  s"-Dpidfile.path=/var/run/${packageName.value}/play.pid",
  // Use separate configuration file for production environment
  s"-Dconfig.file=/usr/share/${packageName.value}/conf/production.conf"
)

maintainer in Linux := "Edoardo Pirovano <edododo_do@yahoo.com>"

packageSummary in Linux := "Wadham ball website"

rpmVendor := "Wadham Ball Committee"

rpmLicense := Some("MIT")

packageDescription := "The website for the Wadham Ball 2016"