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
package com.google.code.sagetvaddons.sre.tasks

import java.util.TimerTask

import org.apache.log4j.Logger

import sagex.api.AiringAPI
import sagex.api.Global
import sagex.api.MediaFileAPI

import com.google.code.sagetvaddons.sre.engine.MonitorTask
import com.google.code.sagetvaddons.sre.plugin.SrePlugin

class MonitorCleanupTask extends TimerTask {
	static private final Logger LOG = Logger.getLogger(MonitorCleanupTask)
	
	@Override
	public void run() {
		if(Global.IsClient()) {
			LOG.warn 'Halting monitor cleanup task: Tasks refuse to run on SageClients'
			return
		}
		def monitors = SrePlugin.INSTANCE.getMonitors()
		def i = 0
		synchronized(monitors) {
			Iterator itr = monitors.iterator()
			while(itr.hasNext()) {
				def mf = AiringAPI.GetMediaFileForAiring(AiringAPI.GetAiringForID(itr.next().airingId))
				if(mf == null || !MediaFileAPI.IsFileCurrentlyRecording(mf)) {
					++i
					itr.remove()
				}
			}
		}
		LOG.debug "Dead monitor threads have been cleaned. [$i removed]"
	}
}
