package org.scalawag.sbt.gitflow

import org.eclipse.jgit.api.Git
/*
class FullRepoGitFlowTest extends GitFlowTest("full-test-repo") {

  val refToVersion = Map(
    "dead" -> "0.0.0-SNAPSHOT",
    "develop" -> "1.2.0-SNAPSHOT",
    "feature/exp" -> "1.2.0-exp-SNAPSHOT",
    "0.1" -> "0.1.0",
    "0.1.1" -> "0.1.1",
    "master" -> "0.2.0",
    "hotfix/0.1.2" -> "0.1.2-SNAPSHOT",
    "hotfix/1" -> "1.1.0-SNAPSHOT",
    "hotfix/1.1" -> "1.1.0-SNAPSHOT",
    "hotfix/1.1.0" -> "1.1.0-SNAPSHOT",
    "hotfix/1.1.0.1" -> "1.1.0-SNAPSHOT",
    "759915cd4db37d082a737700dbf4f624b87784c1" -> "1.1.0-SNAPSHOT",
    "release/0.3.0" -> "0.3.0-SNAPSHOT",
    "release/0.4" -> "0.4.0-SNAPSHOT",
    "release/0.3.1" -> "0.3.1-SNAPSHOT", // Bad version string
    "210a1d024f5b70a8511475bf722bf221df23e6fc" -> classOf[IllegalStateException], // Ambiguous
    "release/0.3.0.1" -> classOf[IllegalStateException], // Bad version string
    "release/1" -> classOf[IllegalStateException], // Bad version string
    "8d4da56dda3a458b6eb4d9c7e5ae15090233f261" -> classOf[IllegalStateException], // No interesting branches or tags
    "6f72338a431ffbca661e3be83fe84e4444d906ef" -> "0.4.0-SNAPSHOT", // Release branch wins over feature and develop
    "d78b19af92e54db6258bcb5d1f606ca34e0cb4b4" -> classOf[IllegalStateException], // This commit has two features.
    "ea6f07fa5c855b96be6dfa16ea6271a906ff06d0" -> "1.2.0-exp-SNAPSHOT", // Feature branch
    "261b126d66689ff0825e54036dad2b26e5d14b0d" -> "0.2.1-SNAPSHOT", // hotfix
    "875bfd3cb74eebba03d31e0f43f00e393b601000" -> "0.1.0", // Tag
    "b700b875ab0e34809566536a0e5e9a2ceb21ef3a" -> "0.1.1" // Tag wins over hotfix.
  )

  refToVersion foreach { case (r,v) =>
    test(s"version($r) -> $v") {
      testRefVersion(r,v)
    }
  }

  test("versionOption(dead) -> None") {
    Git.wrap(gitflow.repository).checkout.setName("dead").call
    gitflow.versionOption shouldEqual None
  }

}
*/