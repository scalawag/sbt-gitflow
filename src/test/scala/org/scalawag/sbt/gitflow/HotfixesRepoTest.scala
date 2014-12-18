package org.scalawag.sbt.gitflow

class HotfixesRepoTest extends GitFlowTest with GitRepoMaker {
  private[this] val git = repo("target/test-git-repos/hotfixes") { implicit git =>
    lod {
      commit {
        feature ; release ; hotfix("blah")
      }
      commit {
        feature ; release ; hotfix("1.0")
      }
      commit {
        feature ; release ; hotfix("2.1.1")
      }
      commit {
        feature ; release ; hotfix("3.1.1","3.1")
      }
      commit {
        feature ; release ; hotfix("4","4.1")
      }
      commit {
        feature ; release ; hotfix("a","5.1","5.1.0")
      }
      lod {
        commit {
          tag("b")
        }
        commit {
          feature ; release ; hotfix("6.0.1")
        }
      }
      lod {
        commit {
          tag("c")
        }
        commit {
          feature ; release ; hotfix("6.0")
        }
      }
      lod {
        commit {
          tag("d")
        }
        commit {
          feature ; release ; hotfix("8.0.0")
        }
      }
    }
  }

  override protected[this] val gitflow = new GitFlow(git.getRepository)

  describe("heedSansMicroRefVersions = false") {

    test("should ignore sans-micro hotfix branch (use coexistent release branch)",
         cfg,
         "hotfix/1.0" -> "3.0.0-SNAPSHOT")

    test("should use single interesting hotfix branch",
         cfg,
         "hotfix/2.1.1" -> "2.1.1-SNAPSHOT")

    test("should use single interesting hotfix branch even if it's not HEAD",
         cfg,
         sha("hotfix/2.1.1") -> "2.1.1-SNAPSHOT")

    test("should find interesting hotfix branch and ignore current sans-micro hotfix branch",
         cfg,
         "hotfix/3.1" -> "3.1.1-SNAPSHOT")

    test("should use current interesting hotfix branch and ignore other sans-micro hotfix branch",
         cfg,
         "hotfix/3.1.1" -> "3.1.1-SNAPSHOT")

    test("should find no interesting branches (use coexistent release branch)",
         cfg,
         "hotfix/4" -> "9.0.0-SNAPSHOT")

    test("should find interesting hotfix branch and ignore all other branches",
         cfg,
         "hotfix/a" -> "5.1.0-SNAPSHOT")

    test("should find interesting hotfix branch and ignore all other branches",
         cfg,
         "hotfix/5.1" -> "5.1.0-SNAPSHOT")

    test("should use current interesting hotfix branch and ignore all other branches",
         cfg,
         "hotfix/5.1.0" -> "5.1.0-SNAPSHOT")

    test("should find interesting descendant hotfix branches",
         cfg,
         "b" -> "6.0.1-SNAPSHOT")

    test("should ignore descendant sans-micro hotfix branches (use descendant release branch)",
         cfg,
         "c" -> "15.0.0-SNAPSHOT")

    test("should use interesting descendant hotfix branches",
         cfg,
         "d" -> "8.0.0-SNAPSHOT")
  }

  describe("heedSansMicroRefVersions = true") {
    val cfg = this.cfg.copy(heedSansMicroRefVersions = true)

    test("should use single interesting sans-micro hotfix branch",
         cfg,
         "hotfix/1.0" -> "1.0.0-SNAPSHOT")

    test("should use single interesting hotfix branch even if it's not HEAD",
         cfg,
         sha("hotfix/1.0") -> "1.0.0-SNAPSHOT")

    test("should use single interesting hotfix branch",
         cfg,
         "hotfix/2.1.1" -> "2.1.1-SNAPSHOT")

    test("should use the current branch even if there are other indications",
         cfg,
         "hotfix/3.1" -> "3.1.0-SNAPSHOT")

    test("should fail due to conflicting interesting hotfix branches",
         cfg,
         sha("hotfix/3.1") -> classOf[IllegalStateException])

    test("should find interesting sans-micro hotfix branch and ignore other branches",
         cfg,
         "hotfix/4" -> "4.1.0-SNAPSHOT")

    test("should use interesting sans-micro hotfix branch and ignore all other branches",
         cfg,
         "hotfix/4.1" -> "4.1.0-SNAPSHOT")

    test("should allow conflicting branches that map to the same artifact version",
         cfg,
         "hotfix/a" -> "5.1.0-SNAPSHOT")

    test("should find interesting descendant branches",
         cfg,
         "b" -> "6.0.1-SNAPSHOT")

    test("should find interesting sans-micro descendant branches",
         cfg,
         "c" -> "6.0.0-SNAPSHOT")

    test("should find interesting descendant branches",
         cfg,
         "d" -> "8.0.0-SNAPSHOT")
  }

  describe("heedSansMicroRefVersions = false, alwaysIncludeMicroInArtifactVersion = false") {
    val cfg = this.cfg.copy(heedSansMicroRefVersions = true,alwaysIncludeMicroInArtifactVersion = false)

    test("should output a sans-micro artifact version if asked to",
         cfg,
         "hotfix/1.0" -> "1.0-SNAPSHOT")

    test("should still output a artifact version with micro if the branch has one",
         cfg,
         "hotfix/8.0.0" -> "8.0.0-SNAPSHOT")

  }

}
