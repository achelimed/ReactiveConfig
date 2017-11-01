package com.github.achelimed.reactiveconfig

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.github.achelimed.reactiveconfig.actors.ConfigReloader.{GetCurrentConfig, Reload}
import com.github.achelimed.reactiveconfig.actors.FileWatcher.Check
import com.github.achelimed.reactiveconfig.actors.{ConfigReloaderSupervisor, FileWatcherSupervisor}
import com.github.achelimed.reactiveconfig.implicits.JavaConversions._
import com.typesafe.config._

import scala.collection.JavaConverters._
import scala.concurrent.duration.{Duration, TimeUnit}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.{implicitConversions, postfixOps}

/**
  * Initial Config when starting (can not be changed)
  */
trait InitialConfig {
  // Initial config
  lazy val Initial: Config = ConfigFactory.load()

  private[reactiveconfig] lazy val (fileWatcherInitialDelay, fileWatcherInterval) = (
    Initial.getDuration("reactive-config.file-watcher.initialDelay").asScala,
    Initial.getDuration("reactive-config.file-watcher.interval").asScala
  )
}

/**
  * The Reactive Config interface that wrap Typesafe Config.
  * In application, create one instance of this trait by supplying an execution context and pass it to all
  * classes that want to get properties from configuration
  */
// scalastyle:off number.of.methods
trait ReactiveConfig extends InitialConfig {

  /**
    * Override this method by supplying your execution context
    *
    * @return ExecutionContext
    */
  implicit def executionContext: ExecutionContext

  // Actor system
  private[reactiveconfig] val actorSystem = ActorSystem(name = "reactive-config-app", defaultExecutionContext = Some(executionContext))

  // Create 2 supervisors, one for each actor
  private val configReloaderSupervisor = actorSystem.actorOf(ConfigReloaderSupervisor.props, ConfigReloaderSupervisor.name)
  private val fileWatcherSupervisor = actorSystem.actorOf(FileWatcherSupervisor.props(configReloaderSupervisor), FileWatcherSupervisor.name)

  // Schedules to send the "Check" message to the FileWatcher actor after "fileWatcherInitialDelay" and every "fileWatcherInterval"
  actorSystem.scheduler.schedule(fileWatcherInitialDelay, fileWatcherInterval) {
    fileWatcherSupervisor ! Check
  }

  // For current method
  private val askTimeout = new Timeout(Initial.getDuration("reactive-config.get-current-config-timeout").asScala)

  /**
    * Force to reload the config from source.
    * Should be used only when necessary, for example by infra ops when they update properties.
    */
  def reload(): Unit = configReloaderSupervisor ! Reload

  /**
    * Get current config (from the last reload)
    */
  def current(): Future[Config] = (configReloaderSupervisor ? GetCurrentConfig) (askTimeout).mapTo[Config]

  def getAnyRefList(path: String): Future[List[_]] = current().map(_.getAnyRefList(path).asScala.toList)

  def getIntList(path: String): Future[List[Int]] = current().map(_.getIntList(path).asScala.map(_.toInt).toList)

  def getValue(path: String): Future[ConfigValue] = current().map(_.getValue(path))

  def root(): Future[ConfigObject] = current().map(_.root())

  def getAnyRef(path: String): Future[AnyRef] = current().map(_.getAnyRef(path))

  def getConfigList(path: String): Future[List[Config]] = current().map(_.getConfigList(path).asScala.toList)

  def getEnumList[T <: Enum[T]](enumClass: Class[T], path: String): Future[List[T]] =
    current().map(_.getEnumList(enumClass, path).asScala.toList)

  def getIsNull(path: String): Future[Boolean] = current().map(_.getIsNull(path))

  def withFallback(other: ConfigMergeable): Future[Config] = current().map(_.withFallback(other))

  def checkValid(reference: Config, restrictToPaths: String*): Future[Unit] = current().map(_.checkValid(reference, restrictToPaths: _*))

  def resolveWith(source: Config): Future[Config] = current().map(_.resolveWith(source))

  def resolveWith(source: Config, options: ConfigResolveOptions): Future[Config] = current().map(_.resolveWith(source, options))

  def getList(path: String): Future[ConfigList] = current().map(_.getList(path))

  def getDouble(path: String): Future[Double] = current().map(_.getDouble(path))

  def getLongList(path: String): Future[List[Long]] = current().map(_.getLongList(path).asScala.map(_.toLong).toList)

  def getObjectList(path: String): Future[List[ConfigObject]] = current().map(_.getObjectList(path).asScala.toList)

  def withOnlyPath(path: String): Future[Config] = current().map(_.withOnlyPath(path))

  def asMap(): Future[Map[String, ConfigValue]] = current().map(_.entrySet()
    .asScala
    .map(entry => (entry.getKey, entry.getValue))
    .toMap[String, ConfigValue]
  )

  def getDoubleList(path: String): Future[List[Double]] = current().map(_.getDoubleList(path).asScala.map(_.toDouble).toList)

  def hasPathOrNull(path: String): Future[Boolean] = current().map(_.hasPathOrNull(path))

  def hasPath(path: String): Future[Boolean] = current().map(_.hasPath(path))

  def getLong(path: String): Future[Long] = current().map(_.getLong(path))

  def getMemorySizeList(path: String): Future[List[ConfigMemorySize]] = current().map(_.getMemorySizeList(path).asScala.toList)

  def getBooleanList(path: String): Future[List[Boolean]] = current().map(_.getBooleanList(path).asScala.map(_.booleanValue()).toList)

  def getBytesList(path: String): Future[List[Long]] = current().map(_.getBytesList(path).asScala.map(_.toLong).toList)

  def getBoolean(path: String): Future[Boolean] = current().map(_.getBoolean(path))

  def getConfig(path: String): Future[Config] = current().map(_.getConfig(path))

  def getObject(path: String): Future[ConfigObject] = current().map(_.getObject(path))

  def getStringList(path: String): Future[List[String]] = current().map(_.getStringList(path).asScala.toList)

  def atPath(path: String): Future[Config] = current().map(_.atPath(path))

  def isEmpty: Future[Boolean] = current().map(_.isEmpty)

  def isResolved: Future[Boolean] = current().map(_.isResolved)

  def atKey(key: String): Future[Config] = current().map(_.atKey(key))

  def getDuration(path: String, unit: TimeUnit): Future[Long] = current().map(_.getDuration(path, unit))

  def getDuration(path: String): Future[Duration] = current().map(_.getDuration(path).asScala)

  def getEnum[T <: Enum[T]](enumClass: Class[T], path: String): Future[T] = current().map(_.getEnum(enumClass, path))

  def withValue(path: String, value: ConfigValue): Future[Config] = current().map(_.withValue(path, value))

  def getInt(path: String): Future[Int] = current().map(_.getInt(path))

  def resolve(): Future[Config] = current().map(_.resolve())

  def resolve(options: ConfigResolveOptions): Future[Config] = current().map(_.resolve(options))

  def getDurationList(path: String, unit: TimeUnit): Future[List[Long]] =
    current().map(_.getDurationList(path, unit).asScala.map(_.toLong).toList)

  def getDurationList(path: String): Future[List[Duration]] = current().map(_.getDurationList(path).asScala.map(_.asScala).toList)

  def origin(): Future[ConfigOrigin] = current().map(_.origin())

  def withoutPath(path: String): Future[Config] = current().map(_.withoutPath(path))

  def getMemorySize(path: String): Future[ConfigMemorySize] = current().map(_.getMemorySize(path))

  def getBytes(path: String): Future[Long] = current().map(_.getBytes(path).toLong)

  def getString(path: String): Future[String] = current().map(_.getString(path))
}
// scalastyle:on number.of.methods