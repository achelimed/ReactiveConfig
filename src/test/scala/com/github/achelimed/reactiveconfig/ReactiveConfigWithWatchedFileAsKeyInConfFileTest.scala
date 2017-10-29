package com.github.achelimed.reactiveconfig

import akka.util.Timeout
import better.files._
import org.scalatest._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Unit Tests
  */
class ReactiveConfigWithWatchedFileAsKeyInConfFileTest extends WordSpec
  with BeforeAndAfterEach
  with Matchers {

  implicit val timeout = Timeout(3 seconds)

  val configFile = System.getProperty("config.file").toFile
  val confFileInitialContent = configFile.contentAsString

  override protected def beforeEach(): Unit = {
    configFile << """
                    |# override the 'reactive-config.file-watcher.path' property before running test
                    |reactive-config.file-watcher.path = target/watch.me
                  """.stripMargin
    // create the file to be watched
    file"target/watch.me".touch()
  }

  override def afterEach(): Unit = {
    configFile < confFileInitialContent
  }

  "ReactiveConfig with a given watched file set in the application-test.conf file" must {

    "do not reload properties until watched file updated" in new ReactiveConfig {
      override implicit def executionContext: ExecutionContext = global

      // initial config
      getString("foo") map { foo =>
        foo should be("this is foo from application-test.conf")
      }

      // update config file
      configFile < "foo = this is NEW foo from application-test.conf"

      // waiting
      Thread.sleep(Initial.getDuration("reactive-config.file-watcher.interval").toMillis)

      // the initial config is still there because we did not touch the 'watch.me' file
      getString("foo") map { sameFoo =>
        sameFoo should be("this is foo from application-test.conf")
      }

      // trigger for reloading the conf
      file"target/watch.me".touch()

      Thread.sleep(Initial.getDuration("reactive-config.file-watcher.interval").toMillis)

      // check the new reloaded
      getString("foo") map { fooModified =>
        fooModified should be("this is NEW foo from application-test.conf")
      }

    }
  }
}
