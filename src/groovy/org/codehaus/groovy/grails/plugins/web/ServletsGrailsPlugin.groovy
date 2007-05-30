/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.plugins.web;
                                                 
import javax.servlet.http.HttpServletRequest
import grails.util.GrailsUtil as GU
import javax.servlet.http.HttpServletResponse
import org.springframework.web.util.*

/**
* <p>This plug-in adds methods to the Servlet API interfaces to make them more Grailsy. For example all classes that implement
* HttpServletRequest will get new methods that allow access to attributes via subscript operator
*
* @author Graeme Rocher
* @since 0.5.5
*/
class ServletsGrailsPlugin {


	def version = GU.getGrailsVersion()
	def dependsOn = [core:version]



	def doWithDynamicMethods = { ctx ->

	    HttpServletRequest.metaClass.getForwardURI = {->
            def result = delegate.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE)
            if(!result) result = delegate.requestURI
            result
	    }

		// enables access to request attributes with request["foo"] syntax
	    HttpServletRequest.metaClass.getAt = { String key ->
	        delegate.getAttribute(key)
	    }         
	    // enables setting of request attributes with request["foo"] = "bar" syntax
	    HttpServletRequest.metaClass.putAt = { String key, Object val ->
	        delegate.setAttribute(key, val)
	    }
        HttpServletRequest.metaClass.isGet = {->
            delegate.method == "GET"
        }        
        HttpServletRequest.metaClass.isPost = {->
            delegate.method == "POST"
        }   
		// enables searching of request attributes with request.find { it.key == 'foo' }
		HttpServletRequest.metaClass.find = { Closure c ->
		   def request = delegate
		   def result = [:]
		   for(name in request.attributeNames) {
				def match = false
				switch(c.parameterTypes.length) {
					case 0:
					  match = c.call()
					break
					case 1:
					   match = c.call(key:name, value:request.getAttribute(name))
					break
					default:
				  	   match =  c.call(name, request.getAttribute(name))
				}
				if(match) {
						result[name] = request.getAttribute(name)
						break
				}
		   }
		   result
		}
		// enables searching of for a number of request attributes using request.findAll { it.key.startsWith('foo') }  
		HttpServletRequest.metaClass.findAll = { Closure c ->
		   def request = delegate 
		   def results = [:]
		   for(name in request.attributeNames) {
				def match = false
				switch(c.parameterTypes.length) {
					case 0:
					  match = c.call()
					break
					case 1: 
					   match = c.call(key:name, value:request.getAttribute(name))
					break
					default:
				  	   match =  c.call(name, request.getAttribute(name))  										
				}                                                                           
				if(match) { results[name] = request.getAttribute(name) }
		   }                                                            
		   results
		}  
		// enables iteration over request attributes with each method request.each { name, value -> }  
		HttpServletRequest.metaClass.each = { Closure c ->
			def request = delegate 
			for(name in request.attributeNames) {
				switch(c.parameterTypes.length) {
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
		// allows the syntax response << "foo"		
		HttpServletResponse.metaClass.leftShift = { Object o ->
			delegate.writer << o
		}
	}
}