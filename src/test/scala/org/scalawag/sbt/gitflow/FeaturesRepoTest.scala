package org.scalawag.sbt.gitflow

class FeaturesRepoTest extends GitFlowTest with GitRepoMaker {
  private[this] val git = repo("target/test-git-repos/features") { implicit git =>

    lod {
      commit {
        feature("a")
      }
      commit {
        feature("b","c")
      }
      lod {
        commit {
          tag("d")
        }
        commit {
          develop
          feature("e")
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

    test("should use single interesting feature branch",
         cfg,
         "feature/a" -> "1.3.0-a-SNAPSHOT")

    test("should use one of two feature branches that is HEAD",
         cfg,
         "feature/b" -> "1.3.0-b-SNAPSHOT")

    test("should fail on two feature branches if neither is HEAD",
         cfg,
         sha("feature/b") -> classOf[IllegalStateException])

    test("should find interesting descendant feature branches",
         cfg,
         "d" -> "1.3.0-e-SNAPSHOT")

  }

  describe("alwaysIncludeMicroInArtifactVersion = false") {
    val cfg = this.cfg.copy(alwaysIncludeMicroInArtifactVersion = false)

    test("should output a sans-micro artifact version if asked to",
         cfg,
         "feature/a" -> "1.3-a-SNAPSHOT")

  }

}
