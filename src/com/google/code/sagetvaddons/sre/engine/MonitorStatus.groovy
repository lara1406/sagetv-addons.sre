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

enum MonitorStatus {
	NO_MONITOR,
	VALID,
	UNKNOWN,
	INVALID,
	MONITORING,
	COMPLETE
	
	static public String getToolTip(String status) {
		if(NO_MONITOR.toString().equals(status))
			return 'Recording not monitored.'
		else if(VALID.toString().equals(status))
			return 'Recording will be monitored.'
		else if(UNKNOWN.toString().equals(status))
			return 'Monitor status unknown.'
		else if(INVALID.toString().equals(status))
			return 'Monitor status is invalid.'
		else if(MONITORING.toString().equals(status))
			return 'This recording is currently being monitored.'
		else if(COMPLETE.toString().equals(status))
			return 'The monitor for this recording has completed.'
		else
			return "Unrecognized status!";
	}
}
