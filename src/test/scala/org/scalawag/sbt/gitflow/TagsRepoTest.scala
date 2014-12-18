package org.scalawag.sbt.gitflow

class TagsRepoTest extends GitFlowTest with GitRepoMaker {
  private[this] val git = repo("target/test-git-repos/tags") { implicit git =>
    lod {
      commit {
        feature ; hotfix ; release ; tag("blah")
      }
      commit {
        feature ; hotfix ; release ; tag("1.0")
      }
      commit {
        feature ; hotfix ; release ; tag("2.1.1")
      }
      commit {
        feature ; hotfix ; release ; tag("3.1.1","3.1")
      }
      commit {
        feature ; hotfix ; release ; tag("4","4.1")
      }
      commit {
        feature ; hotfix ; release ; tag("a","5.1","5.1.0")
      }
      commit {
        tag("b")
      }
      commit {
        tag("6.0.0","7.0")
      }
      commit {
        tag("8.0.0")
      }
      commit {
        feature ; hotfix ; release ; develop
      }
    }
  }

  override protected[this] val gitflow = new GitFlow(git.getRepository)

  describe("heedSansMicroRefVersions = false") {

    test("should ignore sans-micro version tag",
         cfg,
         "1.0" -> "4.0.1-SNAPSHOT")

    test("should use single interesting version tag",
         cfg,
         "2.1.1" -> "2.1.1")

    test("should use single interesting version tag even if it's not HEAD",
         cfg,
         sha("2.1.1") -> "2.1.1")

    test("should find interesting version tag and ignore current sans-micro version tag",
         cfg,
         "3.1" -> "3.1.1")

    test("should use current interesting version tag and ignore other sans-micro version tag",
         cfg,
         "3.1.1" -> "3.1.1")

    test("should find no interesting tags and fall back to looking for hotfix branches",
         cfg,
         "4" -> "13.0.1-SNAPSHOT")

    test("should find interesting version tag and ignore all other tags",
         cfg,
         "a" -> "5.1.0")

    test("should find interesting version tag and ignore all other tags",
         cfg,
         "5.1" -> "5.1.0")

    test("should use current interesting version tag and ignore all other tags",
         cfg,
         "5.1.0" -> "5.1.0")

    test("should ignore interesting descendant tags",
         cfg,
         "b" -> "19.0.1-SNAPSHOT")

  }

  describe("heedSansMicroRefVersions = true") {
    val cfg = this.cfg.copy(heedSansMicroRefVersions = true)

    test("should use single interesting sans-micro version tag",
         cfg,
         "1.0" -> "1.0.0")

    test("should use single interesting version tag even if it's not HEAD",
         cfg,
         sha("1.0") -> "1.0.0")

    test("should use single interesting version tag",
         cfg,
         "2.1.1" -> "2.1.1")

    test("should fail due to conflicting interesting version tags",
         cfg,
         "3.1" -> classOf[IllegalStateException])

    test("should fail due to conflicting interesting version tags",
         cfg,
         "3.1.1" -> classOf[IllegalStateException])

    test("should find interesting sans-micro version tag and ignore other tags",
         cfg,
         "4" -> "4.1.0")

    test("should use interesting sans-micro version tag and ignore all other tags",
         cfg,
         "4.1" -> "4.1.0")

    test("should allow conflicting tags that map to the same artifact version",
         cfg,
         "a" -> "5.1.0")

    test("should ignore interesting descendant tags",
         cfg,
         "b" -> "19.0.1-SNAPSHOT")

  }

  describe("heedSansMicroRefVersions = false, alwaysIncludeMicroInArtifactVersion = false") {
    val cfg = this.cfg.copy(heedSansMicroRefVersions = true,alwaysIncludeMicroInArtifactVersion = false)

    test("should output a sans-micro artifact version if asked to",
         cfg,
         "1.0" -> "1.0")

    test("should still output a artifact version with micro if the tag has one",
         cfg,
         "8.0.0" -> "8.0.0")

  }

}
