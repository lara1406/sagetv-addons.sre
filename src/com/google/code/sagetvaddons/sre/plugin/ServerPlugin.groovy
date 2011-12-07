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

import static com.google.code.sagetvaddons.sre.plugin.SrePlugin.*

import org.apache.log4j.Logger

import sage.SageTVPluginRegistry
import sagex.api.AiringAPI
import sagex.api.Global
import sagex.plugin.AbstractPlugin
import sagex.plugin.PluginProperty
import sagex.plugin.SageEvent

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
final class ServerPlugin implements IPlugin {
	static private final Logger LOG = Logger.getLogger(ServerPlugin)
		
	private Timer timer
	private List monitors
	
	/**
	 * @param registry
	 */
	public ServerPlugin() {
		monitors = Collections.synchronizedList([])
		timer = null
	}
	
	void start() {
		Global.GetCurrentlyRecordingMediaFiles().each {
			createMonitor(null, [MediaFile:it])
		}
		if(timer)
			timer.cancel()
		timer = new Timer(true)
		timer.schedule(new DataStoreCleanupTask(), 10000, 3600000)
		timer.schedule(new MonitorCleanupTask(), 120000, 3600000)
		new Thread(new MonitorValidatorTask()).start()
	}
	
	void stop() {
		synchronized(monitors) {
			for(MonitorThread t : monitors)
				stopThread(t)
		}
		monitors.clear()
		if(timer) {
			timer.cancel()
			timer = null
		}
	}
	
	void validateMonitors(String eventName, Map args) {
		new Thread(new MonitorValidatorTask()).start()
	}
	
	void createMonitor(String eventName, Map args) {
		if(!getMonitor(AiringAPI.GetAiringID(args['MediaFile']))) {
			MonitorThread t = new MonitorThread(args['MediaFile'])
			monitors.add(t)
			t.start()
			LOG.debug "Monitor started for ${args['MediaFile']}"
		}
	}
	
	void stopMonitor(String eventName, Map args) {
		stopThread(getMonitor(AiringAPI.GetAiringID(args['MediaFile'])))
		LOG.debug "Monitor stopped for ${args['MediaFile']}"
	}
	
	void resetMonitor(def airing) {
		def t = getMonitor(AiringAPI.GetAiringID(airing))
		if(t)
			t.setUnmonitored false
	}
	
	List getMonitors() { return monitors }
	
	private MonitorThread getMonitor(int id) {
		synchronized(monitors) {
			for(MonitorThread t : monitors)
				if(t.airingId == id) return t
		}
		return null
	}
	
	private void stopThread(MonitorThread t) {
		if(t) {
			if(t.isAlive())
				t.interrupt()
			monitors.remove t
		}
	}
}