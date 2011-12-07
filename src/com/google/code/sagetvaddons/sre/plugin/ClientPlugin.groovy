/*
*      Copyright 2011 Battams, Derek
*
*       Licensed under the Apache License, Version 2.0 (the "License");
*       you may not use this file except in compliance with the License.
*       You may obtain a copy of the License at
*
*          http://www.apache.org/licenses/LICENSE-2.0
*
*       Unless required by applicable law or agreed to in writing, software
*       distributed under the License is distributed on an "AS IS" BASIS,
*       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*       See the License for the specific language governing permissions and
*       limitations under the License.
*/
package com.google.code.sagetvaddons.sre.plugin

import org.apache.log4j.Logger
import org.apache.log4j.PropertyConfigurator

import sage.SageTVPluginRegistry
import sagex.SageAPI
import sagex.api.AiringAPI
import sagex.api.Global
import sagex.plugin.AbstractPlugin
import sagex.plugin.PluginProperty
import sagex.plugin.SageEvent

import com.google.code.sagetvaddons.sre.engine.DataStore
import com.google.code.sagetvaddons.sre.engine.MonitorThread
import com.google.code.sagetvaddons.sre.plugin.properties.ServerStoredProperty
import com.google.code.sagetvaddons.sre.plugin.validators.IntegerRangeValidator
import com.google.code.sagetvaddons.sre.tasks.DataStoreCleanupTask
import com.google.code.sagetvaddons.sre.tasks.MonitorCleanupTask
import com.google.code.sagetvaddons.sre.tasks.MonitorValidatorTask

/**
 * @author dbattams
 *
 */
public final class ClientPlugin implements IPlugin {
	static private final Logger LOG = Logger.getLogger(ClientPlugin)
		
	void start() { LOG.trace('Plugin start() ignored: Running on client.') }
	
	void stop() { LOG.trace('Plugin stop() ignored: Running on client.') }
	
	void validateMonitors(String eventName, Map args) { LOG.trace('Plugin validateMonitors() ignored: Running on client.') }
	
	void createMonitor(String eventName, Map args) { LOG.trace('Plugin createMonitor() ignored: Running on client.') }
	
	void stopMonitor(String eventName, Map args) { LOG.trace('Plugin stopMonitor() ignored: Running on client.') }
	
	void resetMonitor(def airing) { LOG.trace('Plugin resetMonitor() ignored: Running on client.') }
	
	List getMonitors() { return Collections.EMPTY_LIST }
}