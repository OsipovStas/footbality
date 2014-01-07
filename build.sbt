name := "footbality"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  "postgresql" % "postgresql" % "9.1-901.jdbc4",
  "com.sun.jersey" % "jersey-client" % "1.16",
  "com.sun.jersey" % "jersey-json" % "1.16"
)     

play.Project.playScalaSettings
