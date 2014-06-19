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

  private def localBranches = {
    repository.getAllRefsByPeeledObjectId.get(currentCommit).filter(_.getName.startsWith("refs/heads")).map(_.getName.stripPrefix("refs/heads/")).toList
  }

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

    tags.headOption
  }

  private def mostRecentReleaseVersion = releaseVersions.last

  private def nextReleaseVersion =
    Version(Array(mostRecentReleaseVersion.digits(0), mostRecentReleaseVersion.digits(1) + 1))

  private def developArtifactVersion = {
    val v = nextReleaseVersion
    ArtifactVersion(v.major,v.minor)
  }

  private def releaseArtifactVersion(version:String) = {
    val v = Version.parse(version).get
    ArtifactVersion(v.major,v.minor)
  }

  private def hotfixArtifactVersion(version:String) = {
    val v = Version.parse(version).get
    ArtifactVersion(v.major,v.minor,v.incremental)
  }

  private def featureArtifactVersion(feature:String) = {
    val v = nextReleaseVersion
    ArtifactVersion(v.major,v.minor,feature = Some(feature))
  }

  private def detachedHeadArtifactVersion = {
    def findUniqueBranch(prefix:String, branches:List[String]):Option[String] = {
      branches.filter(_.startsWith(prefix)) match {
        case branch :: Nil => Some(branch)
        case Nil => None
        case _ => throw new IllegalStateException(("your current commit has multiple branches of the same type (so ambiguous): " + branches.mkString(" ")))
      }
    }

    findUniqueBranch("release/", localBranches).flatMap({
      r => Some(releaseArtifactVersion(r.stripPrefix("release/")))
    }) orElse
    findUniqueBranch("feature/", localBranches).flatMap({
      r => Some(featureArtifactVersion(r.stripPrefix("feature/")))
    }) orElse
    findUniqueBranch("develop", localBranches).flatMap({
      r => Some(developArtifactVersion)
    }) orElse
    findUniqueBranch("hotfix/", localBranches).flatMap({
      r => Some(hotfixArtifactVersion(r.stripPrefix("hotfix/")))
    })
  }

  private def taggedArtifactVersion = {
    tagForCurrentCommit flatMap { tag =>
      val v = Version.parse(tag).get
      Some(ArtifactVersion(v.major,v.minor,v.incremental,snapshot = false))
    }
  }

  private def currentBranchArtifactVersion = {
    val Slashed = "([^/]+)/(.+)".r
    currentBranch match {
      case "develop" => Some(developArtifactVersion)
      case Slashed("release", version) => Some(releaseArtifactVersion(version))
      case Slashed("hotfix", version) => Some(hotfixArtifactVersion(version))
      case Slashed("feature", feature) => Some(featureArtifactVersion(feature))
      case _ => None
    }
  }

  /**
   * The version is determined by applying the following rules in order, stopping as soon as one succeeds:
   * 1) If the develop branch is checked out, the version is <next_release_version>-SNAPSHOT
   * 2) If a release branch is checked out, the version is <release-version>-SNAPSHOT
   * 3) If a hotfix branch is checked out, the version is <hotfix-version>-SNAPSHOT
   * 4) If a feature branch is checked out, the version is <next-release-version>-<feature-name>-SNAPSHOT
   * 5) If one and only one version tag refers to the current commit, the version is the same as the tag
   *    (If more than one version tag refers to the commit, an IllegalStateException is thrown)
   * 6) If one and only one release branch refers to the current commit, the version is <release-version>-SNAPSHOT
   *    (If more than one release branch refers to the commit, an IllegalStateException is thrown)
   * 7) If one and only one feature branch refers to the current commit, the version is <next-release-version>-<feature-name>-SNAPSHOT
   *    (If more than one feature branch refers to the commit, an IllegalStateException is thrown)
   * 8) If the develop branch refers to the current commit, the version is <next_release_version>-SNAPSHOT
   * 9) If one and only one hotfix branch refers to the current commit, the version is <hotfix-version>-SNAPSHOT
   *    (If more than one feature branch refers to the commit, an IllegalStateException is thrown)
   *
   * The following patterns are used to identify the branches:
   * release branch: release/<release-version>
   * hotfix branch: hotfix/<hotfix-version>
   * feature branch: feature/<feature-name>
   * develop branch: develop
   *
   * Version tags must match the pattern <major>.<minor>.<optional incremental> Regex: \d+\.\d+(\.\d+)?
   *
   * @return String version
  **/
  def version = {
    currentBranchArtifactVersion orElse taggedArtifactVersion orElse detachedHeadArtifactVersion getOrElse {
      throw new IllegalStateException(s"""Unable to determine version from checked out branch, tags, or other refs.
      |  Working Directory: ${repository.getWorkTree}
      |  Current Commit: ${currentCommit}
      |  Current Branch: ${currentBranch}
      |  Local Branches: ${localBranches.mkString(",")}
      """.stripMargin)
    }
  }
}

object GitFlow {
  def apply():GitFlow = apply(new File(System.getProperty("user.dir")))
  def apply(dir:File):GitFlow = apply((new FileRepositoryBuilder).findGitDir(dir).build)
  def apply(repo:Repository):GitFlow = new GitFlow(repo)

  lazy val WorkingDir = GitFlow()

  def main(args:Array[String]) {
    // If an optional path is passed in as an argument, use that instead of the current directory.
    val dir = args.headOption.map(path => GitFlow(new File(path))).getOrElse(WorkingDir)
    println(dir.version)
  }
}
