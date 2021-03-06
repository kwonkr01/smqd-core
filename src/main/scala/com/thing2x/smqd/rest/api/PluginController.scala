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

package com.thing2x.smqd.rest.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import com.thing2x.smqd.Smqd
import com.thing2x.smqd.plugin._
import com.thing2x.smqd.rest.RestController
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging

import scala.collection.immutable.SortedSet


/**
  * 2018. 7. 6. - Created by Kwon, Yeong Eon
  */
class PluginController(name: String, smqd: Smqd, config: Config) extends RestController(name, smqd, config) with Directives with StrictLogging {

  override def routes: Route = plugins

  private def plugins: Route = {
    ignoreTrailingSlash {
      get {
        parameters('curr_page.as[Int].?, 'page_size.as[Int].?, 'query.as[String].?) { (currPage, pageSize, searchName) =>
          path("packages" / Segment.?) { packageName =>
            getPackages(packageName, searchName, currPage, pageSize)
          } ~
          path("plugins" / Segment / "instances" / Segment.?) { (pluginName, instanceName) =>
            getPluginInstances(pluginName, instanceName, searchName, currPage, pageSize)
          } ~
          path("plugins" / Segment.?) { pluginName =>
            getPlugins(pluginName, searchName, currPage, pageSize)
          }
        }
      }
    }
  }

  private def getPackages(packageName: Option[String], searchName: Option[String], currPage: Option[Int], pageSize: Option[Int]): Route = {
    val pm = smqd.pluginManager
    packageName match {
      case Some(pn) => // exact match
        val rt = pm.packageDefinitions
        rt.find(_.name == pn) match {
          case Some(pkg) =>
            complete(StatusCodes.OK, restSuccess(0, PluginPackageDefinitionFormat.write(pkg)))
          case None =>
            complete(StatusCodes.NotFound, restError(404, s"Package not found: $pn"))
        }
      case None => // search
        searchName match {
          case Some(search) => // query
            val rt = pm.packageDefinitions
            val result = SortedSet[PluginPackageDefinition]() ++ rt.filter(p => p.name.contains(search))
            if (result.isEmpty)
              complete(StatusCodes.NotFound, restError(404, s"Package not found, search $search"))
            else
              complete(StatusCodes.OK, restSuccess(0, pagenate(result, currPage, pageSize)))
          case None => // all
            val result = SortedSet[PluginPackageDefinition]() ++ pm.packageDefinitions
            complete(StatusCodes.OK, restSuccess(0, pagenate(result, currPage, pageSize)))
        }
    }
  }

  private def getPlugins(pluginName: Option[String], searchName: Option[String], currPage: Option[Int], pageSize: Option[Int]): Route = {
    val pm = smqd.pluginManager
    pluginName match {
      case Some(pname) => // exact match
        pm.pluginDefinition(pname) match {
          case Some(p) =>
            complete(StatusCodes.OK, restSuccess(0, PluginDefinitionFormat.write(p)))
          case None =>
            complete(StatusCodes.NotFound, s"Plugin not found plugin: $pname")
        }
      case None => // search
        searchName match {
          case Some(search) => // query
            val result = SortedSet[PluginDefinition]() ++ pm.pluginDefinitions(search)
            if (result.isEmpty)
              complete(StatusCodes.NotFound, restError(404, s"Plugin not found, search $search"))
            else
              complete(StatusCodes.OK, restSuccess(0, pagenate(result, currPage, pageSize)))
          case None => // all
            val result = SortedSet[PluginDefinition]() ++ pm.pluginDefinitions
            complete(StatusCodes.OK, restSuccess(0, pagenate(result, currPage, pageSize)))
        }
    }
  }

  private def getPluginInstances(pluginName: String, instanceName: Option[String], searchName: Option[String], currPage: Option[Int], pageSize: Option[Int]): Route = {
    val pm = smqd.pluginManager
    instanceName match {
      case Some(instName) => // exact match
        pm.instance(pluginName, instName) match {
          case Some(inst) =>
            complete(StatusCodes.OK, restSuccess(0, PluginInstanceFormat.write(inst)))
          case None =>
            complete(StatusCodes.NotFound, s"Plugin instance not found plugin: $pluginName, instance: $instName")
        }
      case None => // search
        searchName match {
          case Some(search) => // query
            val result = SortedSet[PluginInstance[SmqPlugin]]() ++ pm.instances(pluginName, search)
            if (result.isEmpty)
              complete(StatusCodes.NotFound, s"Plugin instance not found plugin: $pluginName, search $search")
            else
              complete(StatusCodes.OK, restSuccess(0, pagenate(result, currPage, pageSize)))
          case None => // all
            val result = SortedSet[PluginInstance[SmqPlugin]]() ++ pm.instances(pluginName)
            complete(StatusCodes.OK, restSuccess(0, pagenate(result, currPage, pageSize)))
        }
    }
  }
}
