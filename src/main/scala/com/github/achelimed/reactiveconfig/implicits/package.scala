package com.github.achelimed.reactiveconfig

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{Duration, FiniteDuration}

package object implicits {

  /**
    * Provide default values for getter methods of ReactiveConfig
    */
  object FutureDefaultValues {

    implicit class FutureWithDefaultValue[T](val future: Future[T])(implicit ec: ExecutionContext) {
      def orDefault(defaultValue: T): Future[T] = future recover { case _ => defaultValue }
    }

  }

  /**
    * All needed type conversions, like: Scala <=> Java
    */
  object JavaConversions {

    implicit class JavaTimeDuration(val duration: java.time.Duration) {
      def asScala: FiniteDuration = Duration.fromNanos(duration.toNanos)
    }

  }

}
