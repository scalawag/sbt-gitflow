package org.scalawag.sbt.gitflow

class OriginGitFlowTest extends GitFlowTest("origin-test-repo") {

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
  }
}
