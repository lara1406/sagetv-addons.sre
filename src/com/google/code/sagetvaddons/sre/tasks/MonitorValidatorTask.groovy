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
import sagex.api.ShowAPI

import com.google.code.sagetvaddons.sre.engine.ClientFactory
import com.google.code.sagetvaddons.sre.engine.DataStore
import com.google.code.sagetvaddons.sre.engine.MonitorStatus
import com.google.code.sagetvaddons.sre.engine.AiringOverride

class MonitorValidatorTask extends TimerTask {
	static private final Logger LOG = Logger.getLogger(MonitorValidatorTask)
	
	@Override
	public void run() {
		if(Global.IsClient()) {
			LOG.warn 'Halting validator task: Tasks refuse to run on SageClients'
			return
		}
		def now = System.currentTimeMillis()
		def ds = DataStore.getInstance()
		Global.GetScheduledRecordings().each {
			def lastCheck = ds.getData(it, DataStore.PROP_LAST_CHECK)
			if(lastCheck == null || lastCheck == '')
				lastCheck = '0'
			lastCheck = lastCheck.toLong()
			if(!lastCheck || ((now - lastCheck) >= 86400000L && AiringAPI.GetScheduleStartTime(it) > now)) {
				LOG.debug "Checking status of ${AiringAPI.GetAiringID(it)}"
				def status
				def clnt = ClientFactory.get()
				def override = ds.getOverrideByObj(it)
				if(!override)
					override = new AiringOverride(id: AiringAPI.GetAiringID(it), title: AiringAPI.GetAiringTitle(it), subtitle: ShowAPI.GetShowEpisode(it), enabled: true)
				try {
					def resp = clnt.getStatus(override.title, override.subtitle, new Date(AiringAPI.GetAiringStartTime(it)))
					if(resp == null)
						status = MonitorStatus.NO_MONITOR
					else if(resp.isError())
						status = MonitorStatus.INVALID
					else
						status = MonitorStatus.VALID
				} catch(IOException e) {
					status = MonitorStatus.UNKNOWN
				}
				if(status != MonitorStatus.UNKNOWN)
					ds.setMonitorStatus it, status
				ds.setData it, DataStore.PROP_LAST_CHECK, System.currentTimeMillis().toString()
			} else if(LOG.isDebugEnabled())
				LOG.debug "Skipped status check of ${AiringAPI.GetAiringID(it)}"
		}
	}
}
