/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.plugins.web.api

/**
* API for classes that access attributes like property access (eg. request.getAttribute('foo') becomes request.foo)
*
* @author Graeme Rocher
* @since 1.4
*
*/

class AttributeAccessor {

	def getProperty(instance, String name) {
		def mp = instance.class.metaClass.getMetaProperty(name)
		return mp ? mp.getProperty(instance) : instance.getAttribute(name)
	}
	
	void setProperty(instance, String name, value) {
		def mp = instance.class.metaClass.getMetaProperty(name)
		if (mp) {
			mp.setProperty(instance, value)
		}
		else {
			instance.setAttribute(name, value)
		}
	}
	
	def getAt(instance, String name) {
		getProperty(instance, name)
	}
	
	def putAt(instance, String name, value) {
		setProperty(instance, name, value)
	}
}
