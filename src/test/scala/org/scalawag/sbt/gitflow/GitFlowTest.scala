package org.scalawag.sbt.gitflow

import org.scalatest.{BeforeAndAfterAll, Matchers, FunSuite}
import java.io.File
import org.eclipse.jgit.api.Git
import net.lingala.zip4j.core._
import com.google.common.io.Files
import org.apache.commons.io.FileUtils

class GitFlowTest extends FunSuite with Matchers with BeforeAndAfterAll {

  private[this] var tmp:File = null
  private[this] var gitflow:GitFlow = null

  override def beforeAll {
    // Unzip our test git repository into a temp directory
    tmp = Files.createTempDir

    val zip = new ZipFile("src/test/resources/test-repo.zip")
    zip.extractAll(tmp.getAbsolutePath)

    gitflow = GitFlow(new File(tmp,"test-repo"))
  }

  override def afterAll {
    FileUtils.deleteDirectory(tmp)
  }

  val refToVersion = Map(
    "develop" -> "0.5-SNAPSHOT",
    "feature/exp" -> "0.5-exp-SNAPSHOT",
    "0.1" -> "0.1",
    "0.1.1" -> "0.1.1",
    "master" -> "0.2",
    "hotfix/0.1.2" -> "0.1.2-SNAPSHOT",
    "hotfix/1" -> classOf[IllegalStateException], // Bad version string
    "hotfix/1.1" -> classOf[IllegalStateException], // Bad version string
    "hotfix/1.1.0" -> classOf[IllegalStateException], // Bad version string
    "hotfix/1.1.0.1" -> classOf[IllegalStateException], // Bad version string
    "release/0.3.0" -> "0.3.0-SNAPSHOT",
    "release/0.4" -> "0.4-SNAPSHOT",
    "release/0.3.1" -> classOf[IllegalStateException], // Bad version string
    "release/0.3.0.1" -> classOf[IllegalStateException], // Bad version string
    "release/1" -> classOf[IllegalStateException], // Bad version string
    "8d4da56dda3a458b6eb4d9c7e5ae15090233f261" -> classOf[IllegalStateException], // No interesting branches or tags
    "6f72338a431ffbca661e3be83fe84e4444d906ef" -> "0.4-SNAPSHOT", // Release branch wins over feature and develop
    "d78b19af92e54db6258bcb5d1f606ca34e0cb4b4" -> classOf[IllegalStateException], // This commit has two features.
    "ea6f07fa5c855b96be6dfa16ea6271a906ff06d0" -> "0.5-exp-SNAPSHOT", // Feature branch
    "261b126d66689ff0825e54036dad2b26e5d14b0d" -> "0.2.1-SNAPSHOT", // hotfix
    "875bfd3cb74eebba03d31e0f43f00e393b601000" -> "0.1", // Tag
    "b700b875ab0e34809566536a0e5e9a2ceb21ef3a" -> "0.1.1" // Tag wins over hotfix.
  )

  refToVersion foreach { case (r,v) =>
    test(s"version($r) -> $v") {
      Git.wrap(gitflow.repository).checkout.setName(r).call
      v match {
        case c:Class[_] =>
          c.isAssignableFrom(intercept[Exception](gitflow.version).getClass) shouldBe true
        case _:String =>
          gitflow.version.toString shouldEqual v
      }
    }
  }

  test("versionOrZero(8d4da56dda3a458b6eb4d9c7e5ae15090233f261) -> 0.0.0-SNAPSHOT") {
    Git.wrap(gitflow.repository).checkout.setName("8d4da56dda3a458b6eb4d9c7e5ae15090233f261").call
    gitflow.versionOrZero.toString shouldEqual "0.0-SNAPSHOT"
  }

  test("versionOrDevelop(8d4da56dda3a458b6eb4d9c7e5ae15090233f261) -> 0.5.0-SNAPSHOT") {
    Git.wrap(gitflow.repository).checkout.setName("8d4da56dda3a458b6eb4d9c7e5ae15090233f261").call
    gitflow.versionOrDevelop.toString shouldEqual "0.5-SNAPSHOT"
  }
}
