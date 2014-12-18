package org.scalawag.sbt.gitflow

class DevelopRepoTest extends GitFlowTest with GitRepoMaker {
  private[this] val git = repo("target/test-git-repos/develop") { implicit git =>
    lod {
      commit {
        tag("init")
      }
      lod {
        commit {
          tag("a")
        }
      }
      lod {
        commit {
          tag("b")
        }
        commit {
          tag("c")
          develop
        }
      }
      lod {
        commit {
          hotfix("1.2.3")
        }
      }
    }
  }

  override protected[this] val gitflow = new GitFlow(git.getRepository)

  describe("alwaysIncludeMicroInArtifactVersion = true") {

    test("should use next develop version with no indicators",
         cfg,
         "a" -> "1.3.0-SNAPSHOT")

    test("should use descendant develop branch",
         cfg,
         "b" -> "1.3.0-SNAPSHOT")

    test("should use coexistent develop branch",
         cfg,
         "c" -> "1.3.0-SNAPSHOT")

  }

  describe("alwaysIncludeMicroInArtifactVersion = false") {
    val cfg = this.cfg.copy(alwaysIncludeMicroInArtifactVersion = false)

    test("should output a sans-micro artifact version if asked to",
         cfg,
         "c" -> "1.3-SNAPSHOT")

  }

  describe("noIndicatorsBehavior = Fail") {
    val cfg = this.cfg.copy(noIndicatorsBehavior = Configuration.NoIndicatorsBehavior.Fail)

    test("should fail with no indicators and fail behavior specified",
         cfg,
         "a" -> classOf[IllegalStateException])

  }

}
