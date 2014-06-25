addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8")

resolvers += "sonatype-oss-releases" at "http://oss.sonatype.org/content/repositories/releases/"

libraryDependencies += "org.scalawag.sbt.gitflow" % "sbt-gitflow" % "1.1.0"
