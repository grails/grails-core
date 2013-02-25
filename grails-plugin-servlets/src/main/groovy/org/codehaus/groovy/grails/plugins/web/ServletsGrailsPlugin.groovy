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
package org.codehaus.groovy.grails.plugins.web

import grails.util.GrailsUtil

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

    def doWithDynamicMethods = { ctx ->
        ServletsGrailsPluginSupport.enhanceServletApi(application.config)
    }
}
