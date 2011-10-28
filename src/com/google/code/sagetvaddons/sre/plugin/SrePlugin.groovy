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
import sagex.plugin.AbstractPlugin
import sagex.plugin.PluginProperty
import sagex.plugin.SageEvent

import com.google.code.sagetvaddons.sre.engine.MonitorThread
import com.google.code.sagetvaddons.sre.plugin.properties.ServerStoredProperty
import com.google.code.sagetvaddons.sre.plugin.validators.IntegerRangeValidator

/**
 * @author dbattams
 *
 */
public final class SrePlugin extends AbstractPlugin {
	static { PropertyConfigurator.configure(!SageAPI.isRemote() ? 'plugins/sre4/' : '' + 'sre4.log4j.properties') }
	static private final Logger LOG = Logger.getLogger(SrePlugin)
	static private SrePlugin INSTANCE = null
	static SrePlugin get() { return INSTANCE }
	
	static final String PLUGIN_ID = 'sre4'
	static private final String PROP_PREFIX = PLUGIN_ID
	static final String PROP_EMAIL = "${PROP_PREFIX}/email"
	static final String PROP_ENABLE = "${PROP_PREFIX}/enable"
	static final String PROP_LIVE_ONLY = "${PROP_PREFIX}/liveOnly"
	static final String PROP_DEFAULT_PAD = "${PROP_PREFIX}/defaultPad"
	static final String PROP_MAX_EXTENSION = "${PROP_PREFIX}/maxExt"
	static final String PROP_POST_PAD = "${PROP_PREFIX}/postGmPad"
	static final String PROP_END_EARLY = "${PROP_PREFIX}/endEarly"
	static final String PROP_RM_MAN_FLAG = "${PROP_PREFIX}/rmManFlag"
	static final String PROP_IGNORE_B2B = "${PROP_PREFIX}/ignoreB2B"
	static final String PROP_GEN_SYSMSG = "${PROP_PREFIX}/genSysMsg"
	
	private List monitors
	
	/**
	 * @param registry
	 */
	public SrePlugin(SageTVPluginRegistry registry) {
		super(registry)
		monitors = Collections.synchronizedList([])
	}
	
	@Override
	void start() {
		super.start()
		PluginProperty p = new ServerStoredProperty(CONFIG_TEXT, PROP_EMAIL, '', 'Email Address', 'Required to submit global overrides.  Must be valid as livepvrddata service sends emails to it.')
		addProperty(p)
		p = new ServerStoredProperty(CONFIG_BOOL, PROP_ENABLE, 'true', 'Enable Event Monitoring', 'Should SRE be monitoring supported events?  If false, SRE monitors nothing.')
		addProperty(p)
		p = new ServerStoredProperty(CONFIG_BOOL, PROP_LIVE_ONLY, 'false', 'Monitor Live Airings Only', 'Only monitor airings that are makred as live in the EPG data unless an override is defined for the airing.')
		addProperty(p)
		p = new ServerStoredProperty(CONFIG_INTEGER, PROP_DEFAULT_PAD, '60', 'Default Padding (mins)', 'Default padding for recordings that should, but can\'t be monitored (network issues, etc.).', new IntegerRangeValidator(0, 360))
		addProperty(p)
		p = new ServerStoredProperty(CONFIG_INTEGER, PROP_MAX_EXTENSION, '8', 'Maximum Extention Time (hours)', 'Maximum number of hours a recording can be extended by this plugin.', new IntegerRangeValidator(1, 16))
		addProperty(p)
		p = new ServerStoredProperty(CONFIG_INTEGER, PROP_POST_PAD, '0', 'Post Game Padding (mins)', 'How many minutes should SRE pad a recording after it\'s over?  Zero means no extra pad.  Use this for \'post game show\' type padding.', new IntegerRangeValidator(0, 120))
		addProperty(p)
		p = new ServerStoredProperty(CONFIG_BOOL, PROP_END_EARLY, 'false', 'End Recordings Early', 'Should SRE end monitored recordings early if the event ends early?')
		addProperty(p)
		p = new ServerStoredProperty(CONFIG_BOOL, PROP_RM_MAN_FLAG, 'true', 'Remove Manual Flag for Favourites', 'Should SRE remove the manual flag for monitored favourites when the recording ends?')
		addProperty(p)
		p = new ServerStoredProperty(CONFIG_BOOL, PROP_IGNORE_B2B, 'false', 'Ignore Back to Back Recordings', 'Should SRE ignore the front half of back to back recordings?')
		addProperty(p)
		p = new ServerStoredProperty(CONFIG_BOOL, PROP_GEN_SYSMSG, 'false', 'Generate System Messages', 'Should SRE generate system messages when overrides may be needed or other errors are detected with the plugin?')
		addProperty(p)
		INSTANCE = this
	}
	
	@SageEvent('RecordingStarted')
	void createMonitor(String eventName, Map args) {
		if(!getMonitor(AiringAPI.GetAiringID(args['MediaFile']))) {
			MonitorThread t = new MonitorThread(args['MediaFile'])
			monitors.add(t)
			t.start()
			LOG.debug "Monitor started for ${args['MediaFile']}"
		}
	}
	
	@SageEvent('RecordingStopped')
	void stopMonitor(String eventName, Map args) {
		MonitorThread t = getMonitor(AiringAPI.GetAiringID(args['MediaFile']))
		if(t) {
			if(t.isAlive())
				t.interrupt()
			monitors.remove t
			LOG.debug "Monitor stopped for ${args['MediaFile']}"
		}
	}
	
	void resetMonitor(def airing) {
		def t = getMonitor(AiringAPI.GetAiringID(airing))
		if(t)
			t.setUnmonitored false
	}
	
	private MonitorThread getMonitor(int id) {
		synchronized(monitors) {
			for(MonitorThread t : monitors)
				if(t.airingId == id) return t
		}
		return null
	}
}