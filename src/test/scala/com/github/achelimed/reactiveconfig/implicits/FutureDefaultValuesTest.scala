package com.github.achelimed.reactiveconfig.implicits

import com.github.achelimed.reactiveconfig.implicits.FutureDefaultValues._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FutureDefaultValuesTest extends WordSpecLike with Matchers with ScalaFutures {

  "DefaultValues" must {

    "provide a default value for a future when this one is failed" in {
      val result = Future.failed(new RuntimeException) orDefault 1
      result.futureValue should be(1)
    }

    "return the value of a successful future even the use of orDefault method" in {
      val result = Future.successful(1) orDefault 2
      result.futureValue should be(1)
    }
  }
}