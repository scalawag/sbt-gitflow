package org.scalawag.sbt.gitflow

import org.eclipse.jgit.revwalk.{RevCommit, RevTag, RevWalk}
import scala.collection.JavaConversions._
import java.io.File
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.lib.Repository

private[gitflow] case class Version(digits:Array[Int]) extends Ordered[Version] {
  require(digits.size >= 2 && digits.size <= 3)

  override def compare(that:Version):Int = {
    // No version (-1) sorts before any version (>= 0)
    this.digits.zipAll(that.digits,-1,-1) map { case (l,r) =>
      l.compare(r)
    } find {
      _ != 0
    } getOrElse {
      0
    }
  }

  val major = digits(0)
  val minor = digits(1)
  val incremental = digits.drop(2).headOption

  override val toString = digits.mkString(".")
}

private[gitflow] object Version {
  private[this] val VersionTag = """\d+\.\d+(\.\d+)?""".r

  private[gitflow] def parse(s:String) =
    if ( VersionTag.pattern.matcher(s).matches )
      Some(Version(s.split('.').map(_.toInt)))
    else
      None
}

case class ArtifactVersion(major:Int,minor:Int,incremental:Option[Int] = Some(0),feature:Option[String] = None,snapshot:Boolean = true) {
  override val toString = {
    val i = incremental.map("." + _).getOrElse("")
    val f = feature.map("-" + _).getOrElse("")
    val s = if (snapshot) "-SNAPSHOT" else ""

    s"$major.$minor$i$f$s"
  }
}

class GitFlow(val repository:Repository) {

  private def currentCommit = repository.resolve("HEAD")

  private def currentBranch = repository.getBranch

  private def releaseVersions = {
    val ReleaseBranchRE = "refs/heads/release/(.*)".r
    val ReleaseTagRE = "refs/tags/(.*)".r

    val knownVersions = repository.getAllRefs.keys flatMap {
      case ReleaseBranchRE(branch) => Some(branch)
      case ReleaseTagRE(tag) => Some(tag)
      case _ => None
    } flatMap Version.parse

    knownVersions.toSeq.sorted
  }

  private def tagForCurrentCommit = {
    val tags = repository.getTags filter {
      case (tag, ref) =>
        // Has to be formatted like a version tag or we don't care about it
        Version.parse(tag).isDefined
    } filter {
      case (tag, ref) =>
        // Has to point to our current commit or we don't care about it.
        val w = new RevWalk(repository)
        val commitId = w.parseAny(ref.getObjectId) match {
          case tag: RevTag => tag.getObject.getId
          case tag: RevCommit => tag.getId
        }
        commitId == currentCommit
    } map {
      case (tag, ref) =>
        // All we really care about is the simple tag name.
        tag //.replaceAllLiterally("refs/tags/", "")
    }

    if (tags.size > 1)
      throw new IllegalStateException("your current commit has multiple version tags (so ambiguous): " + tags.mkString(" "))
    if (tags.size < 1)
      throw new IllegalStateException("your current commit does not have a recognized git flow version tag or branch")

    tags.head
  }

  private def mostRecentReleaseVersion = releaseVersions.last

  private def nextReleaseVersion =
    Version(Array(mostRecentReleaseVersion.digits(0), mostRecentReleaseVersion.digits(1) + 1))

  def version = {
    val Slashed = "([^/]+)/(.+)".r

    currentBranch match {
      case "develop" =>
        val v = nextReleaseVersion
        ArtifactVersion(v.major,v.minor)
      case Slashed("release", version) =>
        val v = Version.parse(version).get
        ArtifactVersion(v.major,v.minor)
      case Slashed("hotfix", version) =>
        val v = Version.parse(version).get
        ArtifactVersion(v.major,v.minor,v.incremental)
      case Slashed("feature", feature) =>
        val v = nextReleaseVersion
        ArtifactVersion(v.major,v.minor,feature = Some(feature))
      case _ =>
        val v = Version.parse(tagForCurrentCommit).get
        ArtifactVersion(v.major,v.minor,v.incremental,snapshot = false)
    }
  }
}

object GitFlow {
  def apply():GitFlow = apply(new File(System.getProperty("user.dir")))
  def apply(dir:File):GitFlow = apply((new FileRepositoryBuilder).findGitDir(dir).build)
  def apply(repo:Repository):GitFlow = new GitFlow(repo)

  lazy val WorkingDir = GitFlow()

  def main(args:Array[String]) {
    println(WorkingDir.version)
  }
}
