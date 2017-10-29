package com.github.achelimed.reactiveconfig.actors

import java.nio.file.{NoSuchFileException, Path, Paths}

import akka.actor.{Actor, ActorLogging, ActorSelection, Props}
import com.github.achelimed.reactiveconfig.InitialConfig
import com.github.achelimed.reactiveconfig.actors.ConfigReloader.Reload

/**
  * File-Watcher actor
  */
class FileWatcher extends Actor with ActorLogging {

  import FileWatcher._

  if (!fileToWatch.underlying.exists()) {
    throw new NoSuchFileException(s"No such file found: $fileToWatch")
  }

  log.info("A FileWatcher is created to watch: {}", fileToWatch)
  var lastUpdateTime: Long = fileToWatch.underlying.lastModified()

  val configReloaderSelection: ActorSelection = context.system.actorSelection("*/config-reloader-supervisor/config-reloader")

  def receive: Receive = {
    case message: FileWatcherMessage => message match {
      case Check =>
        log.debug("Checking file....")
        val lastModified = fileToWatch.underlying.lastModified()
        if (lastModified > lastUpdateTime) {
          lastUpdateTime = lastModified
          log.debug("File has been modified -> Sending 'Reload' message to {}", configReloaderSelection)
          log.info("File has been modified: {}", fileToWatch)
          configReloaderSelection ! Reload
        }
    }
  }

}

object FileWatcher extends InitialConfig {
  // Props
  def props: Props = Props[FileWatcher]

  //
  val FilePathPropKey = "reactive-config.file-watcher.path"

  val fileToWatch: FileToWatch =
    (Option(System.getProperty(FilePathPropKey)), getFilePathPropKeyFromConfigFile(), Option(System.getProperty("config.file"))) match {
      case (Some(filename), _, _) => FileToWatch(Paths.get(filename), "-Dreactive-config.file-watcher.path property system")
      case (_, Some(filename), _) => FileToWatch(Paths.get(filename), "config file > 'reactive-config.file-watcher.path' key")
      case (_, _, Some(filename)) => FileToWatch(Paths.get(filename), "-Dconfig.file property system")
      case _ => FileToWatch(Paths.get("conf/application.conf"), "default behavior")
    }

  private def getFilePathPropKeyFromConfigFile(): Option[String] = {
    if (Initial.hasPathOrNull(FilePathPropKey)) {
      Option(Initial.getString(FilePathPropKey))
    } else {
      None
    }
  }

  final case class FileToWatch(path: Path, origin: String) {
    val underlying = path.toFile

    override def toString = {
      val resolution = if (path.toString != path.toRealPath().toString) s" (resolved to ${path.toRealPath()})" else ""
      s"File: $path$resolution [origin: $origin]"
    }
  }

  // Messages
  sealed trait FileWatcherMessage

  final case object Check extends FileWatcherMessage

}