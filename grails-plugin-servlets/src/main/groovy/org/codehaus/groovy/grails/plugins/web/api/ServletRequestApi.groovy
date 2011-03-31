

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


import javax.servlet.http.HttpServletRequest;
/**
* Additional methods added to the HttpServletRequest API
*
* @author Graeme Rocher
* @since 1.4
*
*/

class ServletRequestApi {

	/**
	 * @return retrieve the forwardURI for the request
	 */
	String getForwardURI(HttpServletRequest instance) {
		org.codehaus.groovy.grails.web.util.WebUtils.getForwardURI(instance)
	}

	/**
	 * @return test whether the current request is an XHR request
	 */
	boolean isXhr(HttpServletRequest instance) {
		instance.getHeader('X-Requested-With') != null
	}

	
	/**
	 * Return true if the request is a get
	 */
	boolean isGet (HttpServletRequest instance) {
		instance.method == "GET"
	}
	
	/**
	 * Return true if the request is a post
	 */
	boolean isPost(HttpServletRequest instance) {
		instance.method == "POST"
	}
	
	/**
	 * enables searching of request attributes with request.find { it.key == 'foo' }
	 */
	def find(HttpServletRequest instance, Closure c) {
		def request = instance
		def result = [:]
		for (name in request.attributeNames) {
			def match = false
			switch (c.parameterTypes.length) {
				case 0:
					match = c.call()
					break
				case 1:
					match = c.call(key:name, value:request.getAttribute(name))
					break
				default:
					match =  c.call(name, request.getAttribute(name))
			}
			if (match) {
				result[name] = request.getAttribute(name)
				break
			}
		}
		result
	}

	/**
	 *  enables searching of for a number of request attributes using request.findAll { it.key.startsWith('foo') }
	 */
	def findAll (HttpServletRequest instance, Closure c) {
		def request = instance
		def results = [:]
		for (name in request.attributeNames) {
			def match = false
			switch (c.parameterTypes.length) {
				case 0:
					match = c.call()
					break
				case 1:
					match = c.call(key:name, value:request.getAttribute(name))
					break
				default:
					match =  c.call(name, request.getAttribute(name))
			}
			if (match) { results[name] = request.getAttribute(name) }
		}
		results
	}

	/**
	 *  enables iteration over request attributes with each method request.each { name, value -> }
	 */
	def each (HttpServletRequest instance, Closure c) {
		def request = instance
		for (name in request.attributeNames) {
			switch (c.parameterTypes.length) {
				case 0:
					c.call()
					break
				case 1:
					c.call(key:name, value:request.getAttribute(name))
					break
				default:
					c.call(name, request.getAttribute(name))
			}
		}
	}
}
