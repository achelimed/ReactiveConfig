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
class ReactiveConfigWithWatchedFileAsPropertySystemTest extends WordSpec
  with BeforeAndAfterEach
  with Matchers {

  implicit val timeout = Timeout(3 seconds)

  System.setProperty("reactive-config.file-watcher.path", "target/watch.me")
  // create the file to watch
  file"target/watch.me".touch()
  // We would set another file to watch : useful for a batch of modifications on our application.conf file
  // When all modifications are set, we touch the watched file, so the config will be reloaded

  val configFile = System.getProperty("config.file").toFile
  val confFileInitialContent = configFile.contentAsString

  override protected def beforeEach(): Unit = {
    configFile < confFileInitialContent
  }

  override def afterEach(): Unit = {
    configFile < confFileInitialContent
  }

  "ReactiveConfig with a given watched file as property system" must {

    "do not reload properties until watched file updated" in new ReactiveConfig {
      override implicit def executionContext: ExecutionContext = global

      // initial config
      getString("foo") map { foo =>
        foo should be("this is foo from application-test.conf")
      }

      // update config file
      configFile < s"foo = this is NEW foo from application-test.conf"

      // waiting
      val intervalBetween2watches = Initial.getDuration("reactive-config.file-watcher.interval").toMillis
      Thread.sleep(intervalBetween2watches)

      // the initial config is still there because we did not touch the 'watch.me' file
      getString("foo") map { sameFoo =>
        sameFoo should be("this is foo from application-test.conf")
      }

      // trigger for reloading the conf
      file"target/watch.me".touch()

      // waiting to be sure that the new config is reloaded
      Thread.sleep(intervalBetween2watches)

      // check the new reloaded
      getString("foo") map { foo =>
        foo should be("this is NEW foo from application-test.conf")
      }

    }
  }
}
