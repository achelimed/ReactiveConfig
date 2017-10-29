package com.github.achelimed.reactiveconfig.actors

import akka.actor.{Actor, ActorLogging, Props}
import com.typesafe.config.{Config, ConfigFactory}

/**
  * Typesafe-Config-Reloader actor
  */
class ConfigReloader extends Actor with ActorLogging {

  import ConfigReloader._

  var config: Config = ConfigFactory.empty()

  override def preStart(): Unit = {
    config = ConfigFactory.load()
    log.info("Initial config reloaded.")
  }

  def receive: Receive = {
    case message: ConfigReloaderMessage => message match {

      case GetCurrentConfig =>
        log.debug("Sending config to {} ...", sender())
        sender() ! config

      case Reload =>
        ConfigFactory.invalidateCaches()
        config = ConfigFactory.load()
        log.info("New config reloaded.")
    }
  }
}

object ConfigReloader {
  // Props
  def props: Props = Props[ConfigReloader]

  // Messages
  sealed trait ConfigReloaderMessage

  final case object GetCurrentConfig extends ConfigReloaderMessage

  final case object Reload extends ConfigReloaderMessage

}