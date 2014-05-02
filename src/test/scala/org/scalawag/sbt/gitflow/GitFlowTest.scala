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
    "develop" -> "0.3-SNAPSHOT",
    "release/0.2" -> "0.2-SNAPSHOT",
    "0.1" -> "0.1",
    "feature/exp" -> "0.3-exp-SNAPSHOT",
    "hotfix/0.1.2" -> "0.1.2-SNAPSHOT",
    "0.1.1" -> "0.1.1",
    "master" -> "0.1.1",
    "8d4da56dda3a458b6eb4d9c7e5ae15090233f261" -> classOf[IllegalStateException]
  )

  refToVersion foreach { case (r,v) =>
    test(s"$r -> $v") {
      Git.wrap(gitflow.repository).checkout.setName(r).call
      v match {
        case c:Class[_] =>
          c.isAssignableFrom(intercept[Exception](gitflow.version).getClass) shouldBe true
        case _:String =>
          gitflow.version.toString shouldEqual v
      }
    }
  }
}
