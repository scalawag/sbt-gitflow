package org.scalawag.sbt.gitflow

class RemotesRepoTest extends GitFlowTest  with GitRepoMaker {
  private[this] val git  =  {

    val remote_a = repo("target/test-git-repos/remote_a") { implicit git =>
      lod {
        commit {
          tag("init")
        }
        lod {
          commit {
            tag("a")
          }
          commit {
            tag("b")
            hotfix("1.0.1")
          }
        }
        lod {
          commit {
            tag("c")
          }
          commit {
            tag("d")
            feature("f1")
          }
        }
        lod {
          commit {
            tag("e")
          }
          commit {
            tag("f")
            release("2.0")
          }
        }
        lod {
          commit {
            tag("g")
          }
          commit {
            tag("h")
            release("3.0.0")
          }
        }
        lod {
          commit {
            tag("i")
          }
          commit {
            tag("j")
            hotfix("1.2.3")
          }
        }
      }
    }

    val remote_b = clone("target/test-git-repos/remote_b","../remote_a")
    remote_b.fetchAll("origin")
//    remote_b.createTrackingBranch("origin","hotfix/1.0.1","release/2.0","feature/f1","develop")
    remote_b.at("j") { implicit git =>
      hotfix("1.2.4")
    }
    remote_b.checkout("master") // HEAD will automatically be create by "remotes"

    val remotes = clone("target/test-git-repos/remotes","../remote_b","b")
    remotes.addRemote("a","../remote_a")

    remotes.fetchAll("b")
    remotes.fetchAll("a")

    remotes
  }

  override protected[this] val gitflow = new GitFlow(git.getRepository)

  describe("heed all remotes") {
//    val cfg = this.cfg.copy(alwaysIncludeMicroInArtifactVersion = false)

    test("should use the descendant hotfix branch from remote 'a'",
         cfg,
         "a" -> "1.0.1-SNAPSHOT")

    test("should use the coexistent hotfix branch from remote 'a'",
         cfg,
         "b" -> "1.0.1-SNAPSHOT")

    test("should use the descendant feature branch from remote 'a'",
         cfg,
         "c" -> "3.1.0-f1-SNAPSHOT")

    test("should use the coexistent feature branch from remote 'a'",
         cfg,
         "d" -> "3.1.0-f1-SNAPSHOT")

    // Should default be the firstDevelop or next develop?  I think the latter.
    test("should ignore descendant sans-micro release branch from remote 'a'",
         cfg,
         "e" -> "3.1.0-SNAPSHOT")

    test("should ignore coexistent sans-micro release branch from remote 'a'",
         cfg,
         "f" -> "3.1.0-SNAPSHOT")

    test("should use the descendant release branch from remote 'a'",
         cfg,
         "g" -> "3.0.0-SNAPSHOT")

    test("should use the coexistent release branch from remote 'a'",
         cfg,
         "h" -> "3.0.0-SNAPSHOT")

    test("should fail due to conflicting remote hotfix branches from 'a' and 'b'",
         cfg,
         "i" -> classOf[IllegalStateException])

  }

  describe("heed no remotes") {
    val cfg = this.cfg.copy(heedRemoteFilter = Set())

    test("should ignore all remote descendant hotfix branches",
         cfg,
         "master" -> "0.1.0-SNAPSHOT")
  }

  describe("heed only remote 'a'") {
    val cfg = this.cfg.copy(heedRemoteFilter = Set("a"))

    test("should use the descendant hotfix branch from remote 'a'",
         cfg,
         "i" -> "1.2.3-SNAPSHOT")
  }

  describe("heed only remote 'b'") {
    val cfg = this.cfg.copy(heedRemoteFilter = Set("b"))

    test("should use the descendant hotfix branch from remote 'b'",
         cfg,
         "i" -> "1.2.4-SNAPSHOT")
  }

/*
  val refToVersion = Map(
    "775f0559b400856b30400e1484b76c59ef3ef111" -> "0.3.0-test-SNAPSHOT",
    "develop" -> "0.3.0-SNAPSHOT",
    "bc7723af4ff5702679454e6f4dad42b881ab8218" -> "0.3.0-SNAPSHOT",
    "d3c667cdd4b4d5152bb833aeeeba054c6cb9b79c" -> "0.2.0-SNAPSHOT",
    "31c12494eb4be74158e127125ba581262a8aaf81" -> "0.1.1-SNAPSHOT"
  )

  refToVersion foreach { case (r,v) =>
    test(s"version($r) -> $v") {
      testRefVersion(r,v)
    }
  }*/
}
