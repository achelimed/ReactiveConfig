package com.github.achelimed.reactiveconfig

import akka.util.Timeout
import better.files._
import org.scalatest._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Integration Tests with application
  */
class ReactiveConfigIT extends WordSpec
  with BeforeAndAfterEach
  with Matchers {

  implicit val timeout = Timeout(3 seconds)

  val applicationConf = (file"conf/application.conf").createIfNotExists(asDirectory = false, createParents = true)

  override protected def beforeEach(): Unit = {
    applicationConf < """foo = "this is foo from conf/application.conf""""
  }

  override def afterEach(): Unit = {
    file"conf".delete(swallowIOExceptions = true)
  }

  "ReactiveConfig without properties system" must {
    //System.setProperty("config.file", "conf/application.conf")

    "load and watch changes from the default 'application.conf' file" in new ReactiveConfig {
      override implicit def executionContext: ExecutionContext = global

      getString("foo") map { foo =>
        foo should be("this is foo from conf/application.conf")
      }

      applicationConf < s"""foo = "this is NEW foo from conf/application.conf""""

      val intervalBetween2watches = Initial.getDuration("reactive-config.file-watcher.interval").toMillis
      Thread.sleep(intervalBetween2watches)

      getString("foo") map { foo =>
        foo should be("this is NEW foo from conf/application.conf")
      }
    }
  }
}
