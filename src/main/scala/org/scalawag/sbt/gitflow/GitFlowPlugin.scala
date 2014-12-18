package org.scalawag.sbt.gitflow

import sbt._
import sbt.Keys._

object GitFlowPlugin extends Plugin {
  val gitFlowArtifactVersion = settingKey[ArtifactVersion]("the inferred artifact version for the git HEAD using git flow semantics")
  val heedSansMicroRefVersions = settingKey[Boolean]("whether to consider branches and tags like 1.0 and release/1.0")
  val alwaysIncludeMicroInArtifactVersion = settingKey[Boolean]("whether to add the trailing micro version to the artifact version always")
  val heedRemoteFilter = settingKey[String => Boolean]("a function that returns true if the named remote should be considered (hint: a Set will work)")
  val firstDevelopVersion = settingKey[GitRefVersion]("the develop artifact version to use before there are any releases")

  class SbtLogger(logger:sbt.Logger) extends Logger {
    override def debug(s:String) = logger.debug(s)
    override def info(s:String) = logger.info(s)
    override def warn(s:String) = logger.warn(s)
    override def error(s:String) = logger.error(s)
  }

  val defaults = Seq(
    heedSansMicroRefVersions := false,
    alwaysIncludeMicroInArtifactVersion := true,
    heedRemoteFilter := { _ => true },
    firstDevelopVersion := GitRefVersion(0,1),
    gitFlowArtifactVersion := {
      val cfg = Configuration(heedSansMicroRefVersions = heedSansMicroRefVersions.value,
                              alwaysIncludeMicroInArtifactVersion = alwaysIncludeMicroInArtifactVersion.value,
                              heedRemoteFilter = heedRemoteFilter.value,
                              firstDevelopVersion = firstDevelopVersion.value,
                              logger = new SbtLogger(sLog.value))

      val gf = GitFlow(baseDirectory.value)

      gf.version(cfg)
    },
    version := gitFlowArtifactVersion.value.toString
  )
}
