package com.github.achelimed.reactiveconfig.actors

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props}
import com.github.achelimed.reactiveconfig.InitialConfig

import scala.concurrent.duration.Duration
import scala.language.postfixOps

/**
  * Conf-Reloader supervisor actor
  */
class ConfigReloaderSupervisor extends Actor with ActorLogging {

  import ConfigReloaderSupervisor._

  // Defining children
  val configReloader: ActorRef = context.actorOf(ConfigReloader.props, "config-reloader")

  // Defining the supervision strategy for the configReloader actor
  override val supervisorStrategy: OneForOneStrategy = OneForOneStrategy(
    maxNrOfRetries = MaxNrOfRetries,
    withinTimeRange = WithinTimeRange) {
    case ex: Throwable =>
      log.debug("ConfigReloader actor encountered an exception : ", ex.getStackTrace)
      log.error(
        """ConfigReloader actor encountered an exception which is '{}'.
          |=> So restarting the actor.""".stripMargin, ex.getMessage)
      Restart
  }

  override def receive: Receive = {
    case message => configReloader forward message
  }
}

object ConfigReloaderSupervisor extends InitialConfig {
  // Name
  val name: String = "config-reloader-supervisor"

  // Props
  def props: Props = Props[ConfigReloaderSupervisor]

  // Constants for strategy
  import com.github.achelimed.reactiveconfig.implicits.JavaConversions._

  val MaxNrOfRetries: Int =
    Initial.getInt("reactive-config.config-reloader.supervisor.one-for-one-strategy.maxNrOfRetries")
  val WithinTimeRange: Duration =
    Initial.getDuration("reactive-config.config-reloader.supervisor.one-for-one-strategy.withinTimeRange").asScala
}