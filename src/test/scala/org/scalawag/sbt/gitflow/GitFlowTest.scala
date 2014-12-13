package org.scalawag.sbt.gitflow

import org.eclipse.jgit.api.Git
import org.scalatest.{Matchers, BeforeAndAfterAll, FunSuite}
import java.io.File
import net.lingala.zip4j.core._
import com.google.common.io.Files
import org.apache.commons.io.FileUtils

abstract class GitFlowTest(repo:String) extends FunSuite with Matchers with BeforeAndAfterAll {
  private[this] var tmp:File = null
  protected[this] var gitflow:GitFlow = null

  override def beforeAll {
    // Unzip our test git repository into a temp directory
    tmp = Files.createTempDir

    val zip = new ZipFile(s"src/test/resources/$repo.zip")
    zip.extractAll(tmp.getAbsolutePath)

    gitflow = GitFlow(new File(tmp,repo))
  }

  override def afterAll {
    FileUtils.deleteDirectory(tmp)
  }

  implicit val cfg = Configuration(heedTwoDigitRefVersions = true,firstDevelopVersion = GitRefVersion(0,0))//,logger = StdoutLogger)

  protected[this] def testRefVersion(ref:String,expected:Any) {
    Git.wrap(gitflow.repository).checkout.setName(ref).call
    expected match {
      case c:Class[_] =>
        c.isAssignableFrom(intercept[Exception](gitflow.version).getClass) shouldBe true
      case _:String =>
        gitflow.version.toString shouldEqual expected
    }
  }

}
