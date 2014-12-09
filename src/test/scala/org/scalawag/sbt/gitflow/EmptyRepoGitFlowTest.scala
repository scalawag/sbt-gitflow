package org.scalawag.sbt.gitflow

class EmptyRepoGitFlowTest extends GitFlowTest("empty-test-repo") {

  test(s"version for repo without release") {
    testRefVersion("develop","0.0-SNAPSHOT")
  }

}
