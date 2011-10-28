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
package com.google.code.sagetvaddons.sre.plugin.engine

import sagex.api.AiringAPI
import sagex.api.ShowAPI
import sagex.api.UserRecordAPI

import com.google.code.livepvrdata4j.Client
import com.google.code.sagetvaddons.sre.plugin.SrePlugin
import com.google.code.sagetvaddons.sre.plugin.engine.MonitorThread.Status

final class DataStore {

	static final String PROP_STATUS = 'status'
	static final String PROP_TITLE = 'title'
	static final String PROP_SUBTITLE = 'subtitle'
	static final String PROP_ENABLED = 'enabled'
	static final String PROP_ID = 'id'
	static final String PROP_LAST_CHECK = 'lastCheck'

	static private final DataStore INSTANCE = new DataStore()

	static DataStore getInstance() {
		return INSTANCE
	}

	private Client clnt

	private DataStore() {
		clnt = new Client()
	}

	private def get(def airing) {
		return UserRecordAPI.GetUserRecord(SrePlugin.PLUGIN_ID, AiringAPI.GetAiringID(airing).toString())
	}

	private String getData(def airing, String name) {
		def record = get(airing)
		return record ? UserRecordAPI.GetUserRecordData(record, name) : null
	}

	private void setData(def airing, String name, def value) {
		UserRecordAPI.SetUserRecordData(UserRecordAPI.AddUserRecord(SrePlugin.PLUGIN_ID, AiringAPI.GetAiringID(airing)), name, value.toString())
	}

	boolean hasOverride(def airing) {
		return getData(airing, PROP_ENABLED)?.length() > 0
	}

	/**
	 * @param id
	 * @return
	 * @deprecated For backwards compatibility only; use the version that accepts an airing object instead
	 */
	boolean deleteOverride(int id) {
		return deleteOverride(AiringAPI.GetAiringForID(id))
	}

	boolean deleteOverride(def airing) {
		def record = get(airing)
		if(record) {
			setData(airing, PROP_TITLE, null)
			setData(airing, PROP_SUBTITLE, null)
			setData(airing, PROP_ENABLED, null)
			setData(airing, PROP_STATUS, clnt.getStatus(AiringAPI.GetAiringTitle(airing), ShowAPI.GetShowEpisode(airing), AiringAPI.GetAiringStartTime(airing)))
			setData(airing, PROP_LAST_CHECK, System.currentTimeMillis())
			return true
		}
		return false
	}

	Status getMonitorStatus(def airing) {
		def status = getData(airing, PROP_STATUS)
		return status ? Status.valueOf(status) : Status.UNKNOWN
	}

	/**
	 * 
	 * @param id
	 * @return
	 * @deprecated Use version that accepts an airing object instead
	 */
	Status getMonitorStatus(int id) {
		return getMonitorStatus(AiringAPI.GetAiringForID(id))
	}

	void setMonitorStatus(def airing, Status status) {
		setData(airing, PROP_STATUS, status)
	}
	
	/**
	 *
	 * @param id
	 * @return
	 * @deprecated Use version that accepts an airing object instead
	 */
	AiringOverride getOverride(int id) {
		return getOverride(AiringAPI.GetAiringForID(id))
	}

	AiringOverride getOverride(def airing) {
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
		UserRecordAPI.GetAllUserRecords(SrePlugin.PLUGIN_ID).each {
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
		return monitorStatusExists(AiringAPI.GetAiringForID(id))
	}

	boolean monitorStatusExists(def airing) {
		return getData(airing, PROP_STATUS)?.length() > 0
	}

	Status newOverride(int id, String title, String subtitle, boolean isEnabled) {
		return newOverride(AiringAPI.GetAiringForID(id), title, subtitle, isEnabled)
	}

	Status newOverride(def airing, String title, String subtitle, boolean isEnabled) {
		def status = isEnabled ? clnt.getStatus(title, subtitle, AiringAPI.GetAiringStartTime(airing)) : null
		if(!isEnabled || !status.isError()) {
			setData(airing, PROP_TITLE, title)
			setData(airing, PROP_SUBTITLE, subtitle)
			setData(airing, PROP_ENABLED, isEnabled)
			setData(airing, PROP_ID, AiringAPI.GetAiringID(airing))
			setData(airing, PROP_STATUS, status ? Status.VALID : Status.NO_MONITOR)
			setData(airing, PROP_LAST_CHECK, System.currentTimeMillis())
			SrePlugin.get().resetMonitor(airing)
		}
	}

	/**
	 * @deprecated For backwards compatibility only; this is now a noop method
	 */
	void close() {}
}