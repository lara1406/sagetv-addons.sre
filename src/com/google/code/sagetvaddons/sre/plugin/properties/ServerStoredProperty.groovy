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
package com.google.code.sagetvaddons.sre.plugin.properties

import sagex.plugin.IPropertyValidator
import sagex.plugin.PluginProperty
import sagex.plugin.ServerPropertyPersistence

/**
 * @author dbattams
 *
 */
class ServerStoredProperty extends PluginProperty {

	/**
	 * @param type
	 * @param setting
	 * @param defaultValue
	 * @param label
	 * @param help
	 */
	public ServerStoredProperty(int type, String setting, String defaultValue,
			String label, String help, IPropertyValidator validator = null) {
		super(type, setting, defaultValue, label, help)
		setPersistence(new ServerPropertyPersistence())
		if(validator)
			setValidator(validator)
	}

	/**
	 * @param type
	 * @param setting
	 * @param defaultValue
	 * @param label
	 * @param help
	 * @param options
	 */
	public ServerStoredProperty(int type, String setting, String defaultValue,
			String label, String help, String[] options, IPropertyValidator validator = null) {
		super(type, setting, defaultValue, label, help, options)
		setPersistence(new ServerPropertyPersistence())
		if(validator)
			setValidator(validator)
	}

	/**
	 * @param type
	 * @param setting
	 * @param defaultValue
	 * @param label
	 * @param help
	 * @param options
	 * @param optionSep
	 */
	public ServerStoredProperty(int type, String setting, String defaultValue,
			String label, String help, String[] options, String optionSep, IPropertyValidator validator = null) {
		super(type, setting, defaultValue, label, help, options, optionSep)
		setPersistence(new ServerPropertyPersistence())
		if(validator)
			setValidator(validator)
	}
}
