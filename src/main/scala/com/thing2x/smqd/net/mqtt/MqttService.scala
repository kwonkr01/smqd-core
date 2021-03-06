// Copyright 2018 UANGEL
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.thing2x.smqd.net.mqtt

import com.thing2x.smqd.Smqd
import com.thing2x.smqd.plugin.Service
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{Channel, ChannelHandler, ChannelOption, EventLoopGroup}
import io.netty.util.ResourceLeakDetector

import scala.util.matching.Regex

/**
  * 2018. 5. 29. - Created by Kwon, Yeong Eon
  */
class MqttService(name: String, smqd: Smqd, config: Config) extends Service(name, smqd, config) with StrictLogging {

  private var _status: Status.Status = Status.UNKNOWN
  override def status: Status.Status = _status

  private var channels: List[Channel] = Nil
  private var groups: List[EventLoopGroup] = Nil

  private val channelThrottlingEnabled = config.getBoolean("throttle.channel.enabled")
  private val readSizeLimit: Long = config.getBytes("throttle.channel.read.size")
  private val readCountLimit: Int = config.getInt("throttle.channel.read.count")
  private val checkInterval: Long = config.getDuration("throttle.channel.check.interval").toMillis
  private val waitMax: Long = config.getDuration("throttle.channel.wait.max").toMillis

  private val channelBpsCounter = new MqttBpsCounter(channelThrottlingEnabled, readSizeLimit, checkInterval, waitMax)
  private val channelTpsCounter = new MqttTpsCounter(channelThrottlingEnabled, readCountLimit, checkInterval)

  private val defaultKeepAliveTime: Int = math.min(config.getDuration("keepalive.time").toMillis/1000, 65536).toInt
  private val messageMaxSize: Int = config.getBytes("message.max.size").toInt

  private val clientIdentifierFormat: Regex = smqd.config.getString("smqd.registry.client.identifier.format").r

  private val metrics = new MqttMetrics(name)

  override def start(): Unit = {
    _status = Status.STARTING
    logger.info(s"Mqtt Service [$name] Starting...")

    val masterGroupThreadCount = config.getInt("thread.master.count")
    val workerGroupThreadCount = config.getInt("thread.worker.count")
    val masterGroup = new NioEventLoopGroup(masterGroupThreadCount)
    val workerGroup = new NioEventLoopGroup(workerGroupThreadCount)
    groups = masterGroup :: workerGroup :: Nil

    val leakDetectorLevel: String = config.getString("leak.detector.level").toUpperCase
    logger.info(s"Setting resource leak detector level to $leakDetectorLevel")
    ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.valueOf(leakDetectorLevel))

    // MQTT
    if ( config.getBoolean("local.enabled") ) {
      val localAddress: String = config.getString("local.address")
      val localPort: Int = config.getInt("local.port")
      val channel = openChannel(masterGroup, workerGroup, localAddress, localPort, createMqttInitializer(name+":mqtt", false))
      channels +:= channel
    }

    // MQTTS
    if ( config.getBoolean("local.secure.enabled") ) {
      val localAddress: String = config.getString("local.secure.address")
      val localPort: Int = config.getInt("local.secure.port")
      val channel = openChannel(masterGroup, workerGroup, localAddress, localPort, createMqttInitializer(name+":mqtts", true))
      channels +:= channel
    }

    // WS
    if ( config.getBoolean("ws.enabled") ) {
      val localAddress: String = config.getString("ws.address")
      val localPort: Int = config.getInt("ws.port")
      val channel = openChannel(masterGroup, workerGroup, localAddress, localPort, createMqttWsInitializer(name+":ws", false))
      channels +:= channel
    }

    // WSS
    if (config.getBoolean("ws.secure.enabled") ) {
      val localAddress: String = config.getString("ws.secure.address")
      val localPort: Int = config.getInt("ws.secure.port")
      val channel = openChannel(masterGroup, workerGroup, localAddress, localPort, createMqttWsInitializer(name+":wss", true))
      channels +:= channel
    }

    logger.info(s"Mqtt Service [$name] Started.")
    _status = Status.RUNNING
  }

  override def stop(): Unit = {
    _status = Status.STOPPING
    logger.info(s"Mqtt Service [$name] Stopping...")
    try {
      channels.foreach(closeChannel)
    } finally {
      groups.foreach(_.shutdownGracefully())
    }
    logger.info(s"Mqtt Service [$name] Stopped.")
    _status = Status.STOPPED
  }

  private def openChannel(masterGroup: EventLoopGroup, workerGroup: EventLoopGroup, localAddress: String, localPort: Int, h: ChannelHandler): Channel = {
    val b = new ServerBootstrap
    b.group(masterGroup, workerGroup).channel(classOf[NioServerSocketChannel])
      .childHandler(h)
      .childOption(ChannelOption.SO_REUSEADDR, new java.lang.Boolean(true))

    val channel = b.bind(localAddress, localPort).sync.channel
    logger.info(s"open channel: ${channel.localAddress.toString}")
    channel
  }

  private def createMqttInitializer(listenerName: String, secure: Boolean): ChannelHandler = {
    new MqttChannelInitializer(
      smqd,
      listenerName,
      if (secure) smqd.tlsProvider else None,
      channelBpsCounter,
      channelTpsCounter,
      messageMaxSize,
      clientIdentifierFormat,
      defaultKeepAliveTime,
      metrics
    )
  }

  private def createMqttWsInitializer(listenerName: String, secure: Boolean): ChannelHandler = {
    new MqttWsChannelInitializer(
      smqd,
      listenerName,
      if (secure) smqd.tlsProvider else None,
      channelBpsCounter,
      channelTpsCounter,
      messageMaxSize,
      clientIdentifierFormat,
      defaultKeepAliveTime,
      metrics
    )
  }

  private def closeChannel(ch: Channel): Unit = {
    logger.info(s"close channel ${ch.localAddress().toString}")
    ch.close.sync
  }

}
