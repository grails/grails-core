/* Copyright 2006-2007 Graeme Rocher
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
package org.codehaus.groovy.grails.plugins.webflow

import grails.util.GrailsUtil

import org.codehaus.groovy.grails.webflow.WebFlowPluginSupport

/**
 * A Grails plug-in that sets up Spring webflow integration.
 *
 * @author Graeme Rocher
 * @since 0.6
 */
class MockWebFlowGrailsPlugin {

    def version = GrailsUtil.getGrailsVersion()
    def dependsOn = [core: version, i18n: version, controllers: version]
    def observe = ['controllers']
    def loadAfter = ['hibernate']

    /**
     * The doWithSpring method of this plug-in registers two beans. The 'flowRegistry" bean which is responsible for storing
     * flows and the 'flowExecutor' bean which is the core of Spring WebFlow and deals with the execution of flows
     */
    def doWithSpring = WebFlowPluginSupport.doWithSpring

    /**
     * Spring WebFlow has its own Map API for some reason so we can add implementations so that they behave like Groovy maps
     * Also we add shortcuts such as flow, conversation and flash for accessing the scopes as the "Scope" suffix seems redundant
     */
    def doWithDynamicMethods = WebFlowPluginSupport.doWithDynamicMethods

    /**
     * Since this plug-in observes the controllers plugin it will receive onChange events when controllers change.
     * This onChange handler will then go through all the flows of the controller, assemble them and re-register
     * with the flow definition registry
     */
    def onChange = WebFlowPluginSupport.onChange
}
