package t2x.smqd

import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorRef
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging

import scala.collection.{SortedSet, mutable}

/**
  * 2018. 6. 22. - Created by Kwon, Yeong Eon
  */
trait Bridge extends LifeCycle with StrictLogging {
  def filterPath: FilterPath
  def driver: BridgeDriver
  def index: Long

  override def toString: String = new StringBuilder("Bridge(")
    .append(driver.name).append(") ")
    .append("[").append(index).append("] ")
    .append(filterPath.toString)
    .toString
}

abstract class AbstractBridge(val driver: BridgeDriver, val index: Long, val filterPath: FilterPath)
  extends Bridge with StrictLogging {

  private var subr: Option[ActorRef] = None

  override def start(): Unit = {
    subr = Some(driver.smqd.subscribe(filterPath, this.bridge))
    logger.info(s"Bridge(${filterPath.toString}) started.")
  }

  override def stop(): Unit = {
    subr match {
      case Some(actor) =>
        driver.smqd.unsubscribe(filterPath, actor)
      case _ =>
    }
    logger.info(s"Bridge(${filterPath.toString}) stopped.")
  }

  def bridge(topic: TopicPath, msg: Any): Unit
}

abstract class BridgeDriver(val name: String, val smqd: Smqd, val config: Config) extends LifeCycle {
  private val indexes: AtomicLong = new AtomicLong()

  private implicit def ordering: Ordering[Bridge] = (x, y) => x.index.compare(y.index)
  protected val bridgeSet: mutable.SortedSet[Bridge] = mutable.SortedSet.empty

  def bridges: SortedSet[Bridge] = bridgeSet

  def addBridge(filterPath: FilterPath, config: Config): Bridge = {
    val b = createBridge(filterPath, config)
    b.start()
    bridgeSet.add(b)
    b
  }

  def removeBridge(index: Long): Option[Bridge] = {
    bridge(index) match {
      case Some(b) =>
        bridgeSet.remove(b)
        b.stop()
        Some(b)
      case _ =>
        None
    }
  }

  def removeBridge(bridge: Bridge): Boolean = {
    bridgeSet.remove(bridge)
  }

  def removeAllBridges(): Unit = {
    bridgeSet.foreach(_.stop())
    bridgeSet.clear()
  }

  def bridge(index: Long): Option[Bridge] = bridgeSet.find(_.index == index)

  def startBridge(index: Long): Option[Bridge] = bridge(index) match {
    case Some(b) =>
      b.start()
      Some(b)
    case None =>
      None
  }

  def stopBridge(index: Long): Option[Bridge] = bridge(index) match {
    case Some(b) =>
      b.stop()
      Some(b)
    case None =>
      None
  }

  protected def createBridge(filterPath: FilterPath, config: Config, index: Long = indexes.getAndIncrement()): Bridge

  override def toString: String = new StringBuilder("BridgeDriver(")
    .append(name).append(") ")
    .append(" has ").append(bridgeSet.size).append(" bridge(s)")
    .toString
}

abstract class AbstractBridgeDriver(name: String, smqd: Smqd, config: Config) extends BridgeDriver(name, smqd, config) with StrictLogging {

  private var _isClosed: Boolean = false
  val isClosed: Boolean = _isClosed

  override def start(): Unit = {
    logger.info(s"BridgeDriver($name) starting...")
    connect()
    logger.info(s"BridgeDriver($name) started.")
  }

  override def stop(): Unit = {
    logger.info(s"BridgeDriver($name) stopping...")
    _isClosed = true
    removeAllBridges()
    disconnect()
    logger.info(s"BridgeDriver($name) stopped.")
  }

  protected def connect(): Unit
  protected def disconnect(): Unit
}