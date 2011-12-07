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

import java.util.jar.JarFile

import sagex.SageAPI

class Version {
	static private final String SRE_VER_ATTR = 'SRE-Version'
	static private final String SRE_PATH_ATTR = 'SRE-SVN-Path'
	static private final String SRE_JAR = (!SageAPI.isRemote() ? 'JARs/' : '') + 'sre.jar'

	static private final Version INSTANCE = new Version()
	static Version get() { return INSTANCE }
	
	private ver
	private svn
	
	Version() {
		File f = new File(SRE_JAR)
		try {
			JarFile jar = new JarFile(f)
			ver = jar.getManifest().getMainAttributes().getValue(SRE_VER_ATTR)
			svn = jar.getManifest().getMainAttributes().getValue(SRE_PATH_ATTR)
		} catch(IOException e) {
			ver = 'Unknown'
			svn = 'Unknown'
		}
	}
	String getVersion() { return ver }	
	String getSvnPath() { return svn }
}
