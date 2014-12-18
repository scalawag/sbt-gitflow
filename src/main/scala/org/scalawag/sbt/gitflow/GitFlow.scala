package org.scalawag.sbt.gitflow

import org.eclipse.jgit.revwalk.RevWalk
import scala.collection.JavaConversions._
import java.io.File
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.lib.Repository

case class Configuration(heedSansMicroRefVersions:Boolean = false,
                         alwaysIncludeMicroInArtifactVersion:Boolean = true,
                         heedRemoteFilter:String => Boolean = { _ => true },
                         firstDevelopVersion:GitRefVersion = GitRefVersion(0,1),
                         logger:Logger = NoopLogger)

case class ArtifactVersion (version:GitRefVersion,feature:Option[String],snapshot:Boolean) {
  def pad(implicit cfg:Configuration):ArtifactVersion = {
    val paddedGitRefVersion =
      if ( cfg.alwaysIncludeMicroInArtifactVersion )
        version.copy(micro = Some(version.micro.getOrElse(0)))
      else
        version
    copy(version = paddedGitRefVersion)
  }

  override val toString = {
    val f = feature.map("-" + _).getOrElse("")
    val s = if (snapshot) "-SNAPSHOT" else ""

    s"$version$f$s"
  }
}

class GitFlow(val repository:Repository) {

  private[this] def currentCommit = repository.resolve("HEAD")

  private[this] def headRefs = repository.getAllRefsByPeeledObjectId.get(currentCommit)

  // This is just a helper function that's used by mapToArtifactVersion because the handling of develop and feature
  // branches is so similar.  Both infer the next version based on the highest release that's known to exist.  The
  // only difference between the two is whether the resulting artifact version has a feature or not.

  private[this] def inferNextVersion(where:String,ref:String,feature:Option[String])(implicit cfg:Configuration):(ArtifactVersion,String) = {
    cfg.logger.debug("Finding and sorting all known releases...")
    val knownVersions = repository.getAllRefs.keys.flatMap {
      case rref @ GitReleaseBranch(branch) => Some(branch.version,rref)
      case rref @ GitHotfixBranch(branch) => Some(branch.version,rref)
      case rref @ GitTag(tag) => Some(tag.version,rref)
      case _ => None
    }.
    // Group by and sort by parsed version.
    groupBy(_._1).toSeq.sortBy(_._1).
    // Make the values contain only the refs that parsed to that version, sorted.
    map { case (version,pairs) =>
      (version,pairs.toSeq.map(_._2).sorted)
    }

    knownVersions.foreach( v => cfg.logger.debug(s"  ${v._1} -> ${v._2.mkString(" ")}") )
    val mostRecentReleaseVersion = knownVersions.lastOption.map(_._1)

    val nextDevelopVersion = mostRecentReleaseVersion map ( _.nextMinorVersion ) getOrElse cfg.firstDevelopVersion

    val av = ArtifactVersion(nextDevelopVersion,feature,true).pad

    mostRecentReleaseVersion match {
      case Some(v) =>
        (av,s"Using version $av due to $where $ref and most recent release $v")
      case None =>
        (av,s"Using version $av due to $where $ref and firstDevelopVersion ${cfg.firstDevelopVersion}")
    }
  }

  // Calculates the artifact version that should be used given the git ref version that we've chosen as the indicator.
  private[this] def mapToArtifactVersion(where:String,ref:GitRef)(implicit cfg:Configuration):(ArtifactVersion,String) =
    ref match {
      case b:GitDevelopBranch => inferNextVersion(where,b.ref,None)
      case b:GitFeatureBranch => inferNextVersion(where,b.ref,Some(b.feature))
      case b:GitVersionBranch =>
        val av = ArtifactVersion(b.version,None,true).pad
        (av,s"Using version $av due to $where ${b.ref}")
      case t:GitTag =>
        val av = ArtifactVersion(t.version,None,false).pad
        (av,s"Using version $av due to $where ${t.ref}")

    }

  // Attempt to find an artifact version based on HEAD.  One of the following will occur:
  //  + HEAD is a branch that is parseable as a version => Some(the artifact version that the version maps to)
  //  + HEAD is not a branch or can't be parsed as a version => None

  private[this] def findCurrentBranchVersion(implicit cfg:Configuration):Option[ArtifactVersion] = {
    cfg.logger.debug("Looking at HEAD to see if it's an interesting branch...")

    repository.getFullBranch match {
      case ref @ GitBranch(branch) =>
        cfg.logger.debug(s"  Y $ref")
        val (av,explanation) = mapToArtifactVersion("HEAD",branch)
        cfg.logger.info(explanation)
        Some(av)
      case ref =>
        cfg.logger.debug(s"  N $ref")
        None
    }
  }

  // All of the following find methods handle the results the same way, so it's factored out into this method for
  // consistency.

  private[this] def handleRefs(where:String,what:String,refs:Iterable[GitRef])(implicit cfg:Configuration):Option[ArtifactVersion] = {
    // Group the refs by their un-remoted version.  Refs that have different remotes are considered equivalent here.
    // Having two of them on one commit does not create ambiguity.

    val refGroups = refs.groupBy(mapToArtifactVersion(where,_)._1)

    if ( refGroups.size > 1 ) {
      cfg.logger.debug("Found multiple candidates, grouping by artifact version...")
      refGroups.foreach { case(out,ins) =>
        cfg.logger.debug(s"  $out <- ${ins.map(_.ref).mkString(" ")}")
      }
    }

    if ( refGroups.isEmpty ) {
      // No ref groups here is always OK.  That just means that this is not how we determine the artifact version.
      None
    } else if ( refGroups.size == 1 ) {
      // Only one ref group here is good.  That means that we've found an unambiguous version to use.
      val (av,explanation) = mapToArtifactVersion(where,refGroups.values.head.head)
      cfg.logger.info(explanation)
      Some(av)
    } else {
      // More than one ref group is ambiguous.  There are refs that imply different versions.  This is bad.
//      cfg.logger.error(s"multiple $where $what render the artifact version ambiguous:")
//      refs foreach { ref =>
//        ref.values.map(_.head.ref).sorted.mkString(" "))
//      cfg.logger
      throw new IllegalStateException(s"multiple $where $what render the artifact version ambiguous: " +
                                        refGroups.values.map(_.head.ref).mkString(" "))
    }
  }

  // Attempt to find an artifact version based on the tags on HEAD.  One of the following will occur:
  //  + no tags on HEAD are parseable as versions => None
  //  + all parseable tags on HEAD map to the same artifact version => Some(version)
  //  + parseable tags on HEAD map to multiple different artifact versions => fail with ambiguity

  private[this] def findTagVersion(implicit cfg:Configuration):Option[ArtifactVersion] = {
    cfg.logger.debug("Looking for version tags on HEAD...")
    val refs = headRefs.map(_.getName).toSeq flatMap {
      case ref @ GitTag(r) =>
        cfg.logger.debug(s"  Y $ref")
        Some(r)
      case ref =>
        cfg.logger.debug(s"  N $ref")
        None
    }

    handleRefs("coexistent","version tags",refs)
  }

  // Attempt to find an artifact version based on the branches that coexist with HEAD.  One of the following will occur:
  //  + no branches coexist with HEAD that are parseable and of the right type => None
  //  + all branches that coexist with HEAD map to the same artifact version => Some(the common version)
  //  + branches that coexist with HEAD map to multiple different artifact versions => fail with ambiguity

  private[this] def findCoexistentBranchVersion(parser:GitBranchParser)(implicit cfg:Configuration):Option[ArtifactVersion] = {
    cfg.logger.debug(s"Looking for ${parser.label} branches that coexist with HEAD...")

    val refs = headRefs.map(_.getName).toSeq flatMap {
      case ref @ parser(r) =>
        cfg.logger.debug(s"  Y $ref")
        Some(r)
      case ref =>
        cfg.logger.debug(s"  N $ref")
        None
    }

    handleRefs("coexistent","branches",refs)
  }

  // Attempt to find an artifact version based on the branches descended from HEAD.  Branches that coexist with
  // HEAD are included, but they are currently detected by findCoexistentBranchVersion prior to this method
  // being called.  One of the following will occur:
  //  + no branches descended from HEAD are both parseable and of the right type => None
  //  + all branches that descend from HEAD map to the same artifact version => Some(the common version)
  //  + branches that descend from HEAD map to multiple artifact versions => fail with ambiguity

  private[this] def findDescendantBranchVersion(parser:GitBranchParser)(implicit cfg:Configuration):Option[ArtifactVersion] = {
    cfg.logger.debug(s"Looking for ${parser.label} branches that descend from HEAD...")

    val walk = new RevWalk(repository)
    walk.setRetainBody(false)
    val current = walk.lookupCommit(currentCommit)

    val refs =
      try {
        repository.getAllRefs.toSeq flatMap {
          case (parser(r),ref) =>
            if ( walk.isMergedInto(current,walk.lookupCommit(ref.getLeaf.getObjectId)) ) {
              cfg.logger.debug(s"  YY ${ref.getName}")
              Some(r)
            } else {
              cfg.logger.debug(s"  YN ${ref.getName}")
              None
            }
          case (_,ref) =>
            cfg.logger.debug(s"  N? ${ref.getName}")
            None
        }
      } finally {
        walk.dispose()
      }

    handleRefs("descendant","branches",refs)
  }

  def versionOption(implicit cfg:Configuration):Option[ArtifactVersion] = {
    cfg.logger.debug("Determining the project version based on git flow state...")
    findCurrentBranchVersion                      orElse
    findTagVersion                                orElse
    findCoexistentBranchVersion(GitHotfixBranch)  orElse
    findCoexistentBranchVersion(GitReleaseBranch) orElse
    findCoexistentBranchVersion(GitFeatureBranch) orElse
    findCoexistentBranchVersion(GitDevelopBranch) orElse
    findDescendantBranchVersion(GitHotfixBranch)  orElse
    findDescendantBranchVersion(GitReleaseBranch) orElse
    findDescendantBranchVersion(GitFeatureBranch) orElse
    findDescendantBranchVersion(GitDevelopBranch)
  }


  def version(implicit cfg:Configuration):ArtifactVersion =
    versionOption getOrElse {
      val defaultVersion = ArtifactVersion(cfg.firstDevelopVersion,None,true).pad
      cfg.logger.info(s"Using default version: ${defaultVersion}")
      defaultVersion
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
    println(dir.version(new Configuration(logger = StdoutLogger)))
  }
}
