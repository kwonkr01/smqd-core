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

package com.thing2x.smqd.plugin.test

import com.thing2x.smqd.{Bridge, FilterPath, Smqd}
import com.thing2x.smqd.plugin.SmqBridgeDriverPlugin
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging

/**
  * 2018. 7. 4. - Created by Kwon, Yeong Eon
  */
class TakeTwoPlugin(name: String, smqd: Smqd, config: Config) extends SmqBridgeDriverPlugin(name, smqd, config) with StrictLogging{

  private var _status: Status.Status = Status.UNKNOWN
  override def status: Status.Status = _status

  override def start(): Unit = {
    _status = Status.STARTING
    logger.info("Start take two plugin")
    _status = Status.RUNNING
  }

  override def stop(): Unit = {
    _status = Status.STOPPING
    logger.info("Stop take two plugin")
    _status = Status.STOPPED
  }

  override protected def connect(): Unit = ???

  override protected def disconnect(): Unit = ???

  override protected def createBridge(filterPath: FilterPath, config: Config, index: Long): Bridge = ???
}
