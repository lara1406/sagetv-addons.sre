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
package com.google.code.sagetvaddons.sre.engine

import org.apache.log4j.Logger

import sagex.api.AiringAPI
import sagex.api.Configuration
import sagex.api.Global
import sagex.api.MediaFileAPI
import sagex.api.ShowAPI

import com.google.code.livepvrdata4j.Client
import com.google.code.sagetvaddons.sre.plugin.SrePlugin
import com.livepvrdata.data.net.resp.Response
import com.livepvrdata.data.net.resp.StatusResponse

class MonitorTask extends TimerTask {
	static private final Logger LOG = Logger.getLogger(MonitorTask)

	static private final int POLL_FREQ = 120000

	final int airingId
	boolean unmonitored

	private def mediaFile
	private Client clnt
	private Response response
	private boolean isExtending
	private boolean defaultPaddingApplied
	private boolean wasMonitored
	private boolean hasOverride

	MonitorTask(def mediaFile) {
		this.mediaFile = mediaFile
		airingId = AiringAPI.GetAiringID(this.mediaFile)
		unmonitored = false
		clnt = ClientFactory.get()
		response = null
		isExtending = false
		defaultPaddingApplied = false
		wasMonitored = false
		hasOverride = false
	}

	synchronized boolean getHasOverride() { return hasOverride }
	
	synchronized void setHasOverride(boolean b) { hasOverride = b }
	
	synchronized boolean isUnmonitored() { return unmonitored }

	synchronized void setUnmonitored(boolean b) {
		unmonitored = b
		MediaFileAPI.SetMediaFileMetadata(mediaFile, DataStore.PROP_MEDIAFILE_MONITORED, Boolean.toString(wasMonitored))
	}

	@Override
	boolean cancel() {
		LOG.trace "${logPreamble()}: Task cancelled"
		return super.cancel()
	}
	
	void run() {
		LOG.debug "${logPreamble()}: Execution started."
		if(Global.IsClient()) {
			LOG.warn 'Halting monitor thread: Monitor threads refuse to run on SageClients!'
			haltMonitor(false, false)
			return
		}
		DataStore ds = DataStore.getInstance()
		if(ds.getMonitorStatusByObj(mediaFile) != MonitorStatus.COMPLETE) {
			if(!MediaFileAPI.IsFileCurrentlyRecording(mediaFile)) {
				LOG.info "${logPreamble()}: Halting monitor because recording has stopped."
				haltMonitor()
				return
			}
			if(!Configuration.GetServerProperty(SrePlugin.PROP_IGNORE_B2B, 'false').toBoolean() || AiringAPI.IsNotManualOrFavorite(AiringAPI.GetAiringOnAfter(mediaFile))) {
				if(!isUnmonitored() || getHasOverride() != ds.hasOverride(mediaFile)) {
					setHasOverride(ds.hasOverride(mediaFile))
					boolean monitorLiveOnly = Boolean.parseBoolean(Configuration.GetServerProperty(SrePlugin.PROP_LIVE_ONLY, 'false'))
					if(SrePlugin.INSTANCE.licensed && !getHasOverride() && monitorLiveOnly && !AiringAPI.IsAiringAttributeSet(mediaFile, 'Live')) {
						setUnmonitored(true)
						LOG.info "${logPreamble()}: Monitor disabled because live only is enabled and there is no override defined."
					} else {
						try {
							def data = getAiringDetails()
							LOG.debug "${logPreamble()}: Fetching status with data: $data"
							response = clnt.getStatus(data[0], data[1], new Date(data[2]))
							if(response == null) {
								wasMonitored = false
								setUnmonitored(true)
								LOG.info "${logPreamble()}: Monitor disabled because it is an unmonitored event."
							} else {
								wasMonitored = true
								setUnmonitored(false)
							}
							processResponse()
						} catch(IOException e) {
							LOG.error "${logPreamble()}: IOError", e
							handleErrorResponse()
						}
					}
				} else if(LOG.isTraceEnabled())
					LOG.trace "${logPreamble()}: Monitor is idle because unmonitored = true and no new override detected."
			} else
				LOG.info "${logPreamble()}: Monitor disabled because ignore back to back is enabled and the next airing is scheduled to record."
		} else
			LOG.warn "${logPreamble()}: Terminating monitor because this airing has already had a completed monitor!"
		LOG.debug "${logPreamble()}: Execution completed."	
	}

	private void rmManualRecFlag() {
		if(wasMonitored && Configuration.GetServerProperty(SrePlugin.PROP_RM_MAN_FLAG, 'true').toBoolean() && AiringAPI.IsFavorite(mediaFile) && AiringAPI.IsManualRecord(mediaFile)) {
			Thread t = new Thread(new Runnable() {
						void run() {
							while(MediaFileAPI.IsFileCurrentlyRecording(mediaFile)) {
								LOG.debug "${logPreamble()}: Waiting to remove manual flag because the recording is still active."
								sleep 15000
							}
							AiringAPI.CancelRecord(mediaFile)
							LOG.info "${logPreamble()}: Manual recording flag removed."
						}
					})
			t.setDaemon true
			t.setName "SREv4-RmManFlag-${airingId}"
			t.start()
		} else if(LOG.isDebugEnabled())
			LOG.debug "${logPreamble()}: Ignored manual flag removal; not required."
		if(LOG.isTraceEnabled())
			LOG.trace "wasMonitored: $wasMonitored, rmManFlag: ${Configuration.GetServerProperty(SrePlugin.PROP_RM_MAN_FLAG, 'true')}, isFav: ${AiringAPI.IsFavorite(mediaFile)}, isMan: ${AiringAPI.IsManualRecord(mediaFile)}"
	}

	private List getAiringDetails() {
		def info = []
		def override = DataStore.getInstance().getOverrideByObj(mediaFile)
		if(override == null) {
			info.add(AiringAPI.GetAiringTitle(mediaFile))
			info.add(ShowAPI.GetShowEpisode(mediaFile))
			LOG.debug "${logPreamble()}: No override found."
		} else {
			info.add(override.title)
			info.add(override.subtitle)
			LOG.info "${logPreamble()}: Override found. $info"
		}
		info.add(AiringAPI.GetAiringStartTime(mediaFile))
		return info
	}

	private void processResponse() {
		if(response == null) return
		DataStore.getInstance().setMonitorStatus(mediaFile, MonitorStatus.MONITORING)
		if(!response.isError()) {
			def resp = (StatusResponse)response
			if(!resp.isComplete()) {
				extendRecording()
			} else {
				stopRecording()
			}
		} else
			handleErrorResponse()
	}

	private void stopRecording() {
		long end
		long postPad = SrePlugin.INSTANCE.licensed ? getPostGamePadding() : 0L
		if(System.currentTimeMillis() < AiringAPI.GetAiringEndTime(mediaFile) && !Configuration.GetServerProperty(SrePlugin.PROP_END_EARLY, 'false').toBoolean()) {
			end = AiringAPI.GetScheduleEndTime(mediaFile) + postPad
			LOG.info "${logPreamble()}: Event is over, but end early is disabled; applying post game padding only."
		} else { // It's early and we do end early OR it's not early, either way, time to stop the recording
			end = System.currentTimeMillis() + 3000L + postPad
			LOG.info "${logPreamble()}: Event is over, applying post game padding from now."
		}
		AiringAPI.SetRecordingTimes(mediaFile, AiringAPI.GetScheduleStartTime(mediaFile), end)
		LOG.info "${logPreamble()}: Recording scheduled to end at ${new Date(end)} [${postPad / 60000L} mins post padding applied]"
		setUnmonitored(true)
		haltMonitor()
	}

	private long getPostGamePadding() {
		return 60000L * Configuration.GetServerProperty(SrePlugin.PROP_POST_PAD, '0').toLong()
	}

	private void extendRecording() {
		if(AiringAPI.GetScheduleEndTime(mediaFile) < System.currentTimeMillis() + POLL_FREQ) {
			long maxExtHrs = Configuration.GetServerProperty(SrePlugin.PROP_MAX_EXTENSION, '8').toLong()
			if(AiringAPI.GetScheduleEndTime(mediaFile) - AiringAPI.GetAiringEndTime(mediaFile) <= (3600000L * maxExtHrs)) {
				AiringAPI.SetRecordingTimes(mediaFile, AiringAPI.GetScheduleStartTime(mediaFile), AiringAPI.GetScheduleEndTime(mediaFile) + POLL_FREQ)
				isExtending = true
				LOG.info "${logPreamble()}: Recording extended."
			} else {
				LOG.warn "${logPreamble()}: Event is not over but max extension time reached!"
			}
		} else
			LOG.info "${logPreamble()}: Event is not over; nothing to be done."
	}

	private void handleErrorResponse() {
		if(isExtending) {
			AiringAPI.SetRecordingTimes(mediaFile, AiringAPI.GetScheduleStartTime(mediaFile), AiringAPI.GetScheduleEndTime(mediaFile) + POLL_FREQ)
			LOG.warn "${logPreamble()}: Error from web service; auto extended recording."
		} else if(!defaultPaddingApplied && DataStore.getInstance().getMonitorStatusByObj(mediaFile) != MonitorStatus.NO_MONITOR) {
			applyDefaultPadding()
			LOG.warn "${logPreamble()}: Error from web service; applied default padding."
		} else
			LOG.warn "${logPreamble()}: Error from web service; safety measures already applied."
	}

	private void applyDefaultPadding(boolean forcePad = false) {
		long pad = Configuration.GetServerProperty(SrePlugin.PROP_DEFAULT_PAD, '0').toLong()
		if(pad > 0 && (response != null || forcePad))
			AiringAPI.SetRecordingTimes(mediaFile, AiringAPI.GetScheduleStartTime(mediaFile), AiringAPI.GetAiringEndTime(mediaFile) + (60000L * pad))
		defaultPaddingApplied = true
	}

	private String logPreamble() {
		return "${airingId}/${AiringAPI.GetAiringTitle(mediaFile)}"
	}
	
	private void haltMonitor(boolean rmFlag = true, boolean setComplete = true) {
		cancel()
		if(rmFlag)
			rmManualRecFlag()
		if(setComplete)
			DataStore.getInstance().setMonitorStatus(mediaFile, MonitorStatus.COMPLETE)		
	}
}
