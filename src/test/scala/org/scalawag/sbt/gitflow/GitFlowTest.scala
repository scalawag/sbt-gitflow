package org.scalawag.sbt.gitflow

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevWalk
import org.scalatest.{FunSpec, Matchers, BeforeAndAfterAll}

abstract class GitFlowTest extends FunSpec with Matchers with BeforeAndAfterAll {
  protected[this] val gitflow:GitFlow

  protected[this] val logger = NoopLogger
  protected[this] val cfg = Configuration(logger = logger)
  protected[this] lazy val walk = new RevWalk(gitflow.repository)

  override protected def afterAll {
    walk.dispose()
  }

  protected[this] def test(description:String,cfg:Configuration,refToExpected:Pair[String,Any]) {
    val (ref,expected) = refToExpected

    it(s"$description: $ref -> $expected") {
      testRefVersion(gitflow,cfg,ref,expected)
    }
  }

  protected[this] def sha(ref:String) = walk.parseCommit(gitflow.repository.getRef(ref).getLeaf.getObjectId).getName

  protected[this] def testRefVersion(gitflow:GitFlow,cfg:Configuration,ref:String,expected:Any) {
    Git.wrap(gitflow.repository).checkout.setName(ref).call
    expected match {
      case c:Class[_] =>
        c.isAssignableFrom(intercept[Exception](gitflow.version(cfg)).getClass) shouldBe true
      case _:String =>
        gitflow.version(cfg).toString shouldEqual expected
    }
  }
}
