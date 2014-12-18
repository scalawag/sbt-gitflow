package org.scalawag.sbt.gitflow

import org.eclipse.jgit.lib.Ref

case class GitRefVersion(major:Int,minor:Int,micro:Option[Int] = None) extends Ordered[GitRefVersion] {
  require(major >= 0 && minor >= 0 && micro.forall( _ >= 0 ),"all version components must be non-negative")

  override def compare(that:GitRefVersion) =
    Stream(
      this.major.compare(that.major),
      this.minor.compare(that.minor),
      this.micro.getOrElse(0).compare(that.micro.getOrElse(0))
    ).find( _ != 0 ).getOrElse(0)

  def nextMinorVersion = this.copy(minor = minor + 1,micro = None)

  override val toString = ( Iterable(major,minor) ++ micro ) mkString "."
}

object GitRefVersion {
  def apply(major:Int,minor:Int,micro:Int) = new GitRefVersion(major,minor,Some(micro))

  private[this] val RE2 = """(\d+)\.(\d+)""".r
  private[this] val RE3 = """(\d+)\.(\d+)\.(\d+)""".r

  def unapply(s:String)(implicit cfg:Configuration):Option[GitRefVersion] =  s match {
    case RE2(major,minor) if cfg.heedSansMicroRefVersions => Some(GitRefVersion(major.toInt,minor.toInt))
    case RE3(major,minor,micro) => Some(GitRefVersion(major.toInt,minor.toInt,micro.toInt))
    case _ => None
  }
}

object GitRemote {
  def unapply(s:String)(implicit cfg:Configuration):Option[Option[String]] =
    if ( s == null )
      // No remote is specified, it's a local branch.
      Some(None)
    else if ( cfg.heedRemoteFilter(s) )
      // A remote is specified and it passes the filter.
      Some(Some(s))
    else
      // A remote is specified but it doesn't pass the filter.
      None
}

sealed trait GitRef {
  val ref:String
}

sealed trait GitBranch extends GitRef {
  val remote:Option[String]
}

trait GitBranchParser {
  val label:String
  def unapply(ref:String)(implicit cfg:Configuration):Option[GitBranch]
}

object GitBranch extends GitBranchParser {
  val label = ""

  def unapply(ref:String)(implicit cfg:Configuration):Option[GitBranch] = ref match {
    case GitDevelopBranch(branch) => Some(branch)
    case GitFeatureBranch(branch) => Some(branch)
    case GitReleaseBranch(branch) => Some(branch)
    case GitHotfixBranch(branch)  => Some(branch)
    case _ => None
  }
}

case class GitDevelopBranch(ref:String,remote:Option[String]) extends GitBranch

object GitDevelopBranch extends GitBranchParser {
  private[this] val RE = "refs/(?:heads|remotes/([^/]+))/develop".r

  val label = "develop"

  def unapply(ref:String)(implicit cfg:Configuration):Option[GitDevelopBranch] = ref match {
    case RE(GitRemote(remote)) => Some(GitDevelopBranch(ref,remote))
    case _ => None
  }
}

case class GitFeatureBranch(ref:String,feature:String,remote:Option[String]) extends GitBranch

object GitFeatureBranch extends GitBranchParser {
  private[this] val RE = "refs/(?:heads|remotes/([^/]+))/feature/([^/]+)".r

  val label = "feature"

  def unapply(ref:String)(implicit cfg:Configuration):Option[GitFeatureBranch] = ref match {
    case RE(GitRemote(remote),feature) => Some(GitFeatureBranch(ref,feature,remote))
    case _ => None
  }
}

case class GitVersionBranch(ref:String,version:GitRefVersion,remote:Option[String]) extends GitBranch

class GitVersionBranchParser(val label:String) extends GitBranchParser {
  private[this] val RE = s"refs/(?:heads|remotes/([^/]+))/$label/([^/]+)".r

  def unapply(ref:String)(implicit cfg:Configuration):Option[GitVersionBranch] = ref match {
    case RE(GitRemote(remote),GitRefVersion(v)) => Some(GitVersionBranch(ref,v,remote))
    case _ => None
  }
}

object GitHotfixBranch extends GitVersionBranchParser("hotfix")
object GitReleaseBranch extends GitVersionBranchParser("release")

case class GitTag(ref:String,version:GitRefVersion) extends GitRef

object GitTag {
  private[this] val RE = "refs/tags/([^/]+)".r

  def unapply(ref:String)(implicit cfg:Configuration):Option[GitTag] = ref match {
    case RE(GitRefVersion(v)) => Some(GitTag(ref,v))
    case _ => None
  }

//  def unapply(ref:Ref)(implicit cfg:Configuration):Option[GitTag] = unapply(ref.getName)
}
