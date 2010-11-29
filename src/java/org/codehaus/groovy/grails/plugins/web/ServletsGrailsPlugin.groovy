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

import grails.util.GrailsUtil

import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

import org.codehaus.groovy.grails.commons.metaclass.MetaClassEnhancer;
import org.codehaus.groovy.grails.plugins.web.api.AttributeAccessor;
import org.codehaus.groovy.grails.plugins.web.api.ServletRequestApi;
import org.springframework.web.util.WebUtils

/**
 * Adds methods to the Servlet API interfaces to make them more Grailsy. For example all classes
 * that implement HttpServletRequest will get new methods that allow access to attributes via
 * subscript operator.
 *
 * @author Graeme Rocher
 * @since 0.5.5
 */
class ServletsGrailsPlugin {

    def version = GrailsUtil.getGrailsVersion()
    def dependsOn = [core:version]
	
	private MetaClassEnhancer requestEnhancer
	private MetaClassEnhancer attrEnhancer
	
	public ServletsGrailsPlugin() {
		this.requestEnhancer = new MetaClassEnhancer()
		requestEnhancer.addApi new ServletRequestApi()
		this.attrEnhancer = new MetaClassEnhancer()
		attrEnhancer.addApi new AttributeAccessor()

	}
    def doWithDynamicMethods = { ctx ->
		requestEnhancer.enhance HttpServletRequest.metaClass
		attrEnhancer.enhanceAll( [ServletContext.metaClass, HttpSession.metaClass] )
        // allows the syntax response << "foo"
        HttpServletResponse.metaClass.leftShift = { Object o ->
            delegate.writer << o
        }
    }
}
