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

publishMavenStyle := true

publishArtifact in Test := false

publishTo <<= version { (v: String) =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { _ => false }

pomExtra :=
  <url>http://github.com/scalawag/sbt-gitflow</url>
  <licenses>
    <license>
      <name>BSD-style</name>
      <url>http://www.opensource.org/licenses/bsd-license.php</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>http://github.com/scalawag/sbt-gitflow.git</url>
    <connection>scm:git:git://github.com/scalawag/sbt-gitflow.git</connection>
  </scm>
  <developers>
    <developer>
      <id>justinp</id>
      <name>Justin Patterson</name>
      <email>justin@scalawag.org</email>
      <url>https://github.com/justinp</url>
    </developer>
  </developers>

