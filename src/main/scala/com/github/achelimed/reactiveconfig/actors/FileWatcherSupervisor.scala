package com.github.achelimed.reactiveconfig.actors

import java.nio.file.NoSuchFileException

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props}
import com.github.achelimed.reactiveconfig.InitialConfig

import scala.concurrent.duration.Duration
import scala.language.postfixOps

/**
  * File-Watcher actor
  */
class FileWatcherSupervisor(subscriber: ActorRef) extends Actor with ActorLogging {

  import FileWatcherSupervisor._

  // Defining children
  val fileWatcher: ActorRef = context.actorOf(FileWatcher.props(subscriber), "file-watcher")

  // Defining the supervision strategy for the fileWatcher actor
  override val supervisorStrategy: OneForOneStrategy = OneForOneStrategy(
    maxNrOfRetries = MaxNrOfRetries,
    withinTimeRange = WithinTimeRange) {
    case nsfe: NoSuchFileException =>
      log.warning(
        """FileWatcher actor does not found the file to watch: '{}'.
          |=> Please, supply a valid file path.
        """.stripMargin, nsfe.getMessage)
      Restart
    case ex: Throwable =>
      log.error(
        """FileWatcher actor encountered an exception which is '{}'.
          |=> The FileWatcher will be stopped.""".stripMargin, ex.getMessage)
      Stop
  }

  def receive: Receive = {
    case message => fileWatcher forward message
  }
}

object FileWatcherSupervisor extends InitialConfig {

  import com.github.achelimed.reactiveconfig.implicits.JavaConversions._

  // Name
  val name: String = "file-watcher-supervisor"

  // Props
  def props(subscriber: ActorRef) = Props(new FileWatcherSupervisor(subscriber))

  // Constants for strategy
  val MaxNrOfRetries: Int =
    Initial.getInt("reactive-config.file-watcher.supervisor.one-for-one-strategy.maxNrOfRetries")
  val WithinTimeRange: Duration =
    Initial.getDuration("reactive-config.file-watcher.supervisor.one-for-one-strategy.withinTimeRange").asScala
}