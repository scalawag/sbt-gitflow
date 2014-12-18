package org.scalawag.sbt.gitflow

class EmptyRepoTest extends GitFlowTest with GitRepoMaker {
  private[this] val git = repo("target/test-git-repos/empty") { implicit git =>
    lod {
      commit {
        develop
      }
    }
  }

  override protected[this] val gitflow = new GitFlow(git.getRepository)

  describe("default firstDevelopVersion") {

    test("should use default version for a commit with no indicators",
         cfg.copy(alwaysIncludeMicroInArtifactVersion = false),
         "master" -> "0.1-SNAPSHOT")

    test("should pad default version with micro when required",
         cfg,
         "master" -> "0.1.0-SNAPSHOT")

    test("should use firstDevelopVersion for develop with no releases",
         cfg.copy(alwaysIncludeMicroInArtifactVersion = false),
         "develop" -> "0.1-SNAPSHOT")

    test("should pad firstDevelopVersion with micro when required",
         cfg,
         "develop" -> "0.1.0-SNAPSHOT")

  }

  describe("specified firstDevelopVersion (1.2)") {
    val cfg = this.cfg.copy(firstDevelopVersion = GitRefVersion(1,2))

    test("should use default version for a commit with no indicators",
         cfg.copy(alwaysIncludeMicroInArtifactVersion = false),
         "master" -> "1.2-SNAPSHOT")

    test("should pad default version with micro if required (specified default)",
         cfg,
         "master" -> "1.2.0-SNAPSHOT")

    test("should use firstDevelopVersion for develop with no releases (specified default)",
         cfg.copy(alwaysIncludeMicroInArtifactVersion = false),
         "develop" -> "1.2-SNAPSHOT")

    test("should pad firstDevelopVersion with micro if required (specified default)",
         cfg,
         "develop" -> "1.2.0-SNAPSHOT")

  }
}
