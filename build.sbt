organization := "org.scalawag.sbt.gitflow"

name := "sbt-gitflow"

version := "1.0.0"

crossPaths := false

libraryDependencies ++= Seq(
  "org.eclipse.jgit" % "org.eclipse.jgit" % "3.3.2.201404171909-r",
  "org.scalatest" %% "scalatest" % "2.1.0" % "test",
  "net.lingala.zip4j" % "zip4j" % "1.3.2" % "test",
  "com.google.guava" % "guava-io" % "r03" % "test",
  "commons-io" % "commons-io" % "2.4" % "test"
)
