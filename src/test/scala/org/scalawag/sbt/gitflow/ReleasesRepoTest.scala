package org.scalawag.sbt.gitflow

class ReleasesRepoTest extends GitFlowTest with GitRepoMaker {
  private[this] val git = repo("target/test-git-repos/releases") { implicit git =>
    lod {
      commit {
        feature ; release("blah")
      }
      commit {
        feature ; release("1.0")
      }
      commit {
        feature ; release("2.1.1")
      }
      commit {
        feature ; release("3.1.1","3.1")
      }
      commit {
        feature ; release("4","4.1")
      }
      commit {
        feature ; release("a","5.1","5.1.0")
      }
      lod {
        commit {
          tag("b")
        }
        commit {
          feature ; release("6.0.1")
        }
      }
      lod {
        commit {
          tag("c")
        }
        commit {
          feature ; release("6.0")
        }
      }
      lod {
        commit {
          tag("d")
        }
        commit {
          feature ; release("8.0.0")
        }
      }
    }
  }

  override protected[this] val gitflow = new GitFlow(git.getRepository)

  describe("heedSansMicroRefVersions = false") {

    test("should ignore sans-micro release branch (use coexistent feature branch)",
         cfg,
         "release/1.0" -> "8.1.0-f1-SNAPSHOT")

    test("should use single interesting release branch",
         cfg,
         "release/2.1.1" -> "2.1.1-SNAPSHOT")

    test("should use single interesting release branch even if it's not HEAD",
         cfg,
         sha("release/2.1.1") -> "2.1.1-SNAPSHOT")

    test("should find interesting release branch and ignore current sans-micro release branch",
         cfg,
         "release/3.1" -> "3.1.1-SNAPSHOT")

    test("should use current interesting release branch and ignore other sans-micro release branch",
         cfg,
         "release/3.1.1" -> "3.1.1-SNAPSHOT")

    test("should find no interesting branches (use coexistent feature branch)",
         cfg,
         "release/4" -> "8.1.0-f4-SNAPSHOT")

    test("should find interesting release branch and ignore all other branches",
         cfg,
         "release/a" -> "5.1.0-SNAPSHOT")

    test("should find interesting release branch and ignore all other branches",
         cfg,
         "release/5.1" -> "5.1.0-SNAPSHOT")

    test("should use current interesting release branch and ignore all other branches",
         cfg,
         "release/5.1.0" -> "5.1.0-SNAPSHOT")

    test("should find interesting descendant release branches",
         cfg,
         "b" -> "6.0.1-SNAPSHOT")

    test("should ignore descendant sans-micro release branches (use descendant feature branch)",
         cfg,
         "c" -> "8.1.0-f7-SNAPSHOT")

    test("should not find interesting sans-micro descendant branches (use feature branch)",
         cfg,
         "d" -> "8.0.0-SNAPSHOT")
  }

  describe("heedSansMicroRefVersions = true") {
    val cfg = this.cfg.copy(heedSansMicroRefVersions = true)

    test("should use single interesting sans-micro release branch",
         cfg,
         "release/1.0" -> "1.0.0-SNAPSHOT")

    test("should use single interesting release branch even if it's not HEAD",
         cfg,
         sha("release/1.0") -> "1.0.0-SNAPSHOT")

    test("should use single interesting release branch",
         cfg,
         "release/2.1.1" -> "2.1.1-SNAPSHOT")

    test("should use the current branch even if there are other indications",
         cfg,
         "release/3.1" -> "3.1.0-SNAPSHOT")

    test("should fail due to conflicting interesting release branches",
         cfg,
         sha("release/3.1") -> classOf[IllegalStateException])

    test("should find interesting sans-micro release branch and ignore other branches",
         cfg,
         "release/4" -> "4.1.0-SNAPSHOT")

    test("should use interesting sans-micro release branch and ignore all other branches",
         cfg,
         "release/4.1" -> "4.1.0-SNAPSHOT")

    test("should allow conflicting branches that map to the same artifact version",
         cfg,
         "release/a" -> "5.1.0-SNAPSHOT")

    test("should find interesting descendant branches",
         cfg,
         "b" -> "6.0.1-SNAPSHOT")

    test("should find interesting sans-micro descendant branches",
         cfg,
         "c" -> "6.0.0-SNAPSHOT")

    test("should find interesting descendant branches (use descendant feature branch)",
         cfg,
         "d" -> "8.0.0-SNAPSHOT")

  }

  describe("heedSansMicroRefVersions = false, alwaysIncludeMicroInArtifactVersion = false") {
    val cfg = this.cfg.copy(heedSansMicroRefVersions = true,alwaysIncludeMicroInArtifactVersion = false)

    test("should output a sans-micro artifact version if asked to",
         cfg,
         "release/1.0" -> "1.0-SNAPSHOT")

    test("should still output a artifact version with micro if the branch has one",
         cfg,
         "release/8.0.0" -> "8.0.0-SNAPSHOT")

  }

}
