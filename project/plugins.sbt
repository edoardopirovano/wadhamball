resolvers ++= Seq(
  "RoundEights" at "http://maven.spikemark.net/roundeights",
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
  "central" at "http://repo1.maven.org/maven2/"
)

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.0")

// Use the LESS sbt plugin for LESS compilation
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.0")