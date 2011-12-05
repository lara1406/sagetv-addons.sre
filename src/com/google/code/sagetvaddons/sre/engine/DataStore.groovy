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
import sagex.api.MediaFileAPI
import sagex.api.ShowAPI
import sagex.api.UserRecordAPI

final class DataStore {
	static private final Logger LOG = Logger.getLogger(DataStore)
	
	static final String STORE_ID = 'sre4'
	static final String PROP_STATUS = 'status'
	static final String PROP_TITLE = 'title'
	static final String PROP_SUBTITLE = 'subtitle'
	static final String PROP_ENABLED = 'enabled'
	static final String PROP_ID = 'id'
	static final String PROP_LAST_CHECK = 'lastCheck'
	static final String PROP_MEDIAFILE_MONITORED = 'SREv4_Monitored'

	static private final DataStore INSTANCE = new DataStore()

	static DataStore getInstance() {
		return INSTANCE
	}

	private DataStore() {}

	private def get(def airing) {
		return UserRecordAPI.GetUserRecord(STORE_ID, AiringAPI.GetAiringID(airing).toString())
	}

	private String getData(def airing, String name) {
		def record = get(airing)
		return record ? UserRecordAPI.GetUserRecordData(record, name) : null
	}

	private void setData(def airing, String name, def value) {
		def id = AiringAPI.GetAiringID(airing).toString()
		UserRecordAPI.SetUserRecordData(UserRecordAPI.AddUserRecord(STORE_ID, id), name, value.toString())
		UserRecordAPI.SetUserRecordData(UserRecordAPI.AddUserRecord(STORE_ID, id), PROP_ID, id)
	}

	// NOTE: Arg can be a MediaFile OR an Airing
	boolean wasMonitored(def mediaFile) {
		return mediaFile != null ? Boolean.parseBoolean(MediaFileAPI.GetMediaFileMetadata(mediaFile, PROP_MEDIAFILE_MONITORED)) : false
	}
	
	void clean() {
		UserRecordAPI.GetAllUserRecords(STORE_ID).each { r ->
			def id = UserRecordAPI.GetUserRecordData(r, PROP_ID).toInteger()
			def air = AiringAPI.GetAiringForID(id)
			if(!air || (AiringAPI.GetScheduleStartTime(air) < System.currentTimeMillis() && !MediaFileAPI.IsFileCurrentlyRecording(AiringAPI.GetMediaFileForAiring(air)))) {
				if(UserRecordAPI.DeleteUserRecord(r))
					LOG.info "Deleted UserRecord for $id"
				else
					LOG.error "Failed to delete UserRecord for $id"
			}
		}
	}
	
	boolean hasOverride(def airing) {
		return getData(airing, PROP_ENABLED)?.length() > 0
	}

	/**
	 * @param id
	 * @return
	 * @deprecated For backwards compatibility only; use deleteOverrideByObj() instead
	 */
	boolean deleteOverride(int id) {
		return deleteOverrideByObj(AiringAPI.GetAiringForID(id))
	}

	boolean deleteOverrideByObj(def airing) {
		def record = get(airing)
		if(record) {
			setData(airing, PROP_TITLE, null)
			setData(airing, PROP_SUBTITLE, null)
			setData(airing, PROP_ENABLED, null)
			setData(airing, PROP_STATUS, MonitorStatus.UNKNOWN)
			setData(airing, PROP_LAST_CHECK, 0)
			return true
		}
		return false
	}

	MonitorStatus getMonitorStatusByObj(def airing) {
		LOG.info "Calling getMonitorStatus() $airing"
		def status = getData(airing, PROP_STATUS)
		return status ? MonitorStatus.valueOf(status) : MonitorStatus.UNKNOWN
	}

	/**
	 * 
	 * @param id
	 * @return
	 * @deprecated Use getMonitorStatusByObj() instead
	 */
	MonitorStatus getMonitorStatus(int id) {
		LOG.info "Calling getMonitorStatus(int: $id)"
		return getMonitorStatusByObj(AiringAPI.GetAiringForID(id))
	}

	void setMonitorStatus(def airing, MonitorStatus status) {
		setData(airing, PROP_STATUS, status)
	}
	
	/**
	 *
	 * @param id
	 * @return
	 * @deprecated Use version that accepts an airing object instead
	 */
	AiringOverride getOverride(int id) {
		return getOverrideByObj(AiringAPI.GetAiringForID(id))
	}

	AiringOverride getOverrideByObj(def airing) {
		def title = getData(airing, PROP_TITLE)
		def subtitle = getData(airing, PROP_SUBTITLE)
		def enabled = getData(airing, PROP_ENABLED)
		if(title || subtitle || enabled) {
			if(!title) title = AiringAPI.GetAiringTitle(airing)
			if(!subtitle) subtitle = ShowAPI.GetShowEpisode(airing)
			if(!enabled) enabled = Boolean.TRUE.toString()
			return new AiringOverride(id: AiringAPI.GetAiringID(airing), title: title, subtitle: subtitle, enabled: Boolean.parseBoolean(enabled))
		}
		return null
	}

	private AiringOverride getOverrideForUserRecord(def record) {
		def id = UserRecordAPI.GetUserRecordData(record, PROP_ID)
		return id ? getOverride(AiringAPI.GetAiringForID(id.toInteger())) : null
	}

	AiringOverride[] getOverrides() {
		def overrides = []
		UserRecordAPI.GetAllUserRecords(STORE_ID).each {
			def override = getOverrideForUserRecord(it)
			if(override)
				overrides.add(override)
		}
		return overrides
	}

	/**
	 * 
	 * @return
	 * @deprecated For backwards compatibility only; should no longer be used; always returns true
	 */
	boolean isGoogleAccountRegistered() { return true }

	/**
	 * 
	 * @return
	 * @deprecated For backwards compatibility only; always returns true
	 */
	boolean isValid() { return true }

	boolean monitorStatusExists(int id) {
		return monitorStatusExistsForObj(AiringAPI.GetAiringForID(id))
	}

	boolean monitorStatusExistsForObj(def airing) {
		return getData(airing, PROP_STATUS)?.length() > 0
	}

	MonitorStatus newOverride(int id, String title, String subtitle, boolean isEnabled) {
		return newOverrideForObj(AiringAPI.GetAiringForID(id), title, subtitle, isEnabled)
	}

	MonitorStatus newOverrideForObj(def airing, String title, String subtitle, boolean isEnabled) {
		setData(airing, PROP_TITLE, title)
		setData(airing, PROP_SUBTITLE, subtitle)
		setData(airing, PROP_ENABLED, isEnabled)
		setData(airing, PROP_STATUS, MonitorStatus.UNKNOWN)
		setData(airing, PROP_LAST_CHECK, 0)
	}

	/**
	 * @deprecated For backwards compatibility only; this is now a noop method
	 */
	void close() {}
}