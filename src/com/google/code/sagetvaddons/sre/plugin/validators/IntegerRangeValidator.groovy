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
package com.google.code.sagetvaddons.sre.plugin.validators

import sagex.plugin.IPropertyValidator;

/**
 * @author dbattams
 *
 */
class IntegerRangeValidator implements IPropertyValidator {

	final int min
	final int max
	
	/**
	 * 
	 */
	public IntegerRangeValidator(int min, int max = Integer.MAX_VALUE) {
		this.min = min
		this.max = max
	}

	/* (non-Javadoc)
	 * @see sagex.plugin.IPropertyValidator#validate(java.lang.String, java.lang.String)
	 */
	@Override
	public void validate(String setting, String val) throws Exception {
		def num = val.toInteger();
		if(num < min || num > max)
			throw new IllegalArgumentException("Value must be between $min and $max, inclusive!");
	}

}
