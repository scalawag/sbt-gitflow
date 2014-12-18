package org.scalawag.sbt.gitflow

import java.io.File

import org.eclipse.jgit.api.{CreateBranchCommand, Git}
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.util.FileUtils

trait GitRepoMaker {
  private[this] var nextTempBranchNumber = 0
  private[this] var nextCommitNumber = 0
  private[this] var nextBranchNumber = 0

  private[this] def getBranchNumber = {
    val answer = nextBranchNumber
    nextBranchNumber += 1
    answer
  }

  protected[this] def commit(fn: => Unit)(implicit git:Git) {
    nextCommitNumber += 1
    git.commit.setMessage(s"c$nextCommitNumber").call
    fn
  }

  protected[this] def tag(tags:String*)(implicit git:Git):Unit =
    tags foreach { tag =>
      git.tag.setName(tag).call()
    }

  protected[this] def branch(branches:String*)(implicit git:Git):Unit =
    branches foreach { branch =>
      git.branchCreate.setName(branch).call
    }

  protected[this] def hotfix(names:String*)(implicit git:Git):Unit =
    branch(names.map( name => s"hotfix/$name" ):_*)

  protected[this] def hotfix(implicit git:Git):Unit =
    hotfix(s"$getBranchNumber.0.1")

  protected[this] def release(names:String*)(implicit git:Git):Unit =
    branch(names.map( name => s"release/$name" ):_*)

  protected[this] def release(implicit git:Git):Unit =
    release(s"$getBranchNumber.0.0")

  protected[this] def feature(names:String*)(implicit git:Git):Unit =
    branch(names.map( name => s"feature/$name" ):_*)

  protected[this] def feature(implicit git:Git):Unit =
    feature(s"f$getBranchNumber")

  protected[this] def develop(implicit git:Git):Unit =
    branch("develop")

  protected[this] def lod(fn: => Unit)(implicit git:Git) {
    val branchName = s"__lod__$nextTempBranchNumber"
    nextTempBranchNumber += 1

    if ( git.getRepository.resolve("HEAD") == null ) {
      fn
    } else {
      git.checkout.setCreateBranch(true).setName(branchName).call()
      fn
      git.checkout.setName("master").call()
      git.branchDelete.setBranchNames(branchName).setForce(true).call()
    }
  }

  protected[this] def repo(repoDirName:String)(fn: Git => Unit) = {
    val repoDir = new File(repoDirName)

    // Make sure any old repo has been removed before we create the new one.
    if ( repoDir.exists )
      FileUtils.delete(repoDir,FileUtils.RECURSIVE)

    val git = Git.init.setDirectory(repoDir).call
    fn(git)
    git
  }

  protected[this] def clone(repoDirName:String,pathToRemote:String,name:String = "origin") = {
    val repoDir = new File(repoDirName)

    // Make sure any old repo has been removed before we create the new one.
    if ( repoDir.exists )
      FileUtils.delete(repoDir,FileUtils.RECURSIVE)

    Git.cloneRepository().setRemote(name).setURI(pathToRemote).setDirectory(repoDir).call()
  }

  protected implicit class GitPimper(git:Git) {
    def at(ref:String)(fn: Git => Unit) {
      git.checkout().setName(ref).call()
      fn(git)
    }

    def checkout(ref:String) {
      git.checkout().setName(ref).call()
    }

    def addRemote(name:String,pathToRemote:String): Unit = {
      val config = git.getRepository.getConfig
      config.setString("remote",name,"url",pathToRemote)
      config.save()
    }

    def fetch(remote:String,refs:String*) {
      git.fetch.setRemote(remote).setRefSpecs(refs.map(new RefSpec(_)):_*).call()
    }

    def fetchAll(remote:String) =
      fetch(remote,s"refs/heads/*:refs/remotes/$remote/*")

    def createTrackingBranch(remote:String,names:String*) {
      names foreach { name =>
        git.branchCreate.
          setName(name).
          setStartPoint(s"remotes/$remote/$name").
          setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).call()
      }
    }
  }
}
