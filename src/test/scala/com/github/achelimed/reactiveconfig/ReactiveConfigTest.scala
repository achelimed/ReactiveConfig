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
class ReactiveConfigTest extends WordSpec
  with BeforeAndAfterEach
  with Matchers {

  implicit val timeout = Timeout(3 seconds)

  val configFile = System.getProperty("config.file").toFile
  val confFileInitialContent = configFile.contentAsString

  override protected def beforeEach(): Unit = {
    configFile < confFileInitialContent
  }

  override def afterEach(): Unit = {
    configFile < confFileInitialContent
  }

  "ReactiveConfig with 'config.file' property system set" must {

    "load and watch changes from this file" in new ReactiveConfig {
      override implicit def executionContext: ExecutionContext = global

      getString("foo") map { foo =>
        foo should be("this is foo from application-test.conf")
      }

      configFile < s"foo = this is NEW foo from application-test.conf"
      configFile << s"\nbar = this is bar from application-test.conf"

      Thread.sleep(Initial.getDuration("reactive-config.file-watcher.interval").toMillis)

      for {
        foo <- getString("foo")
        bar <- getString("bar")
      } yield {
        foo should be("this is NEW foo from application-test.conf")
        bar should be("this is bar from application-test.conf")
      }
    }

    "reload configuration when force it" in new ReactiveConfig {
      override implicit def executionContext: ExecutionContext = global

      getString("foo") map { foo =>
        foo should be("this is foo from conf/application.conf")
      }

      configFile < s"foo = this is NEW foo from application-test.conf"

      reload()

      getString("foo") map { foo =>
        foo should be("this is NEW foo from application-test.conf")
      }
    }
  }
}
