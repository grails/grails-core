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

import org.springframework.webflow.engine.RequestControlContext
import org.springframework.webflow.core.collection.MutableAttributeMap
/**
 * A Grails plug-in that sets up Spring webflow integration
 
 * @author Graeme Rocher
 * @since 0.6
  *
 * Created: Jul 3, 2007
 * Time: 8:05:55 AM
 * 
 */


import org.springframework.webflow.definition.registry.FlowDefinitionRegistry
import org.codehaus.groovy.grails.commons.GrailsControllerClass
import org.springframework.webflow.engine.builder.FlowAssembler
import org.codehaus.groovy.grails.webflow.engine.builder.FlowBuilder
import org.springframework.webflow.definition.registry.StaticFlowDefinitionHolder


class WebFlowGrailsPlugin {

    def version = grails.util.GrailsUtil.getGrailsVersion()
    def dependsOn = [core:version,i18n:version]
    def observe = ['controllers']
    def loadAfter = ['hibernate']

    /**
     * The doWithSpring method of this plug-in registers two beans. The 'flowRegistry" bean which is responsible for storing
     * flows and the 'flowExecutor' bean which is the core of Spring WebFlow and deals with the execution of flows
     */
    def doWithSpring = {
        flowRegistry(org.codehaus.groovy.grails.webflow.engine.builder.ControllerFlowRegistry) {
            grailsApplication = ref("grailsApplication", true)
        }
        flowScopeRegistrar(org.springframework.webflow.config.scope.ScopeRegistrar)        
        boolean hasExecutionListener = false
        if(manager.hasGrailsPlugin('hibernate') ) {
            try {
                hibernateConversationListener(org.codehaus.groovy.grails.webflow.persistence.SessionAwareHibernateFlowExecutionListener, sessionFactory, transactionManager)
                executionListenerLoader(org.springframework.webflow.execution.factory.StaticFlowExecutionListenerLoader, hibernateConversationListener)
                hasExecutionListener = true
                sessionFactory.currentSessionContextClass = org.codehaus.groovy.grails.webflow.persistence.FlowAwareCurrentSessionContext                
            } catch (MissingPropertyException mpe) {
                // no session factory, this is ok
                log.info "Webflow loading without Hibernate integration. SessionFactory not found."
            }

        }
        def repoType = (application.config.grails.webflow.flow.storage == "client")  ? "CLIENT" : "CONTINUATION"
        flowExecutor(org.codehaus.groovy.grails.webflow.config.GrailsAwareFlowExecutorFactoryBean) {
            definitionLocator = flowRegistry
            repositoryType = repoType
            grailsApplication = ref("grailsApplication", true)
            if(manager.hasGrailsPlugin('hibernate') && hasExecutionListener) {
                executionListenerLoader = executionListenerLoader
            }
        }
    }

    /**
     * Spring WebFlow has its own Map API for some reason so we can add implementations so that they behave like Groovy maps
     * Also we add shortcuts such as flow, conversation and flash for accessing the scopes as the "Scope" suffix seems redundant
     */
    def doWithDynamicMethods = {
        RequestControlContext.metaClass.getFlow = {->
            delegate.flowScope
        }
        RequestControlContext.metaClass.getConversation = {->
            delegate.conversationScope
        }
        RequestControlContext.metaClass.getFlash = {->
            delegate.flashScope
        }

        MutableAttributeMap.metaClass.getProperty = { String name ->
            def mp = delegate.class.metaClass.getMetaProperty(name)
            def result = null
            if(mp) result = mp.getProperty(delegate)
            else {
                result = delegate.get(name)
            }
            result
        }
        MutableAttributeMap.metaClass.setProperty = { String name, value ->
            def mp = delegate.class.metaClass.getMetaProperty(name)
            if(mp) mp.setProperty(delegate, value)
            else {
                delegate.put(name, value)
            }
        }
        MutableAttributeMap.metaClass.clear = {-> delegate.asMap().clear() }
        MutableAttributeMap.metaClass.getAt = { String key -> delegate.get(key) }
        MutableAttributeMap.metaClass.putAt = { String key, value -> delegate.put(key,value) }
    }

    /**
     * Since this plug-in observes the controllers plugin it will receive onChange events when controllers change.
     * This onChange handler will then go through all the flows of the controller, assemble them and re-register
     * with the flow definition registry
     */
    def onChange = { event ->
        FlowDefinitionRegistry flowRegistry = event.ctx.flowRegistry
        def controller = application.getControllerClass(event.source.name)
		if(controller) {
	        for(flow in controller.flows) {
	            def FlowBuilder builder = new FlowBuilder(flow.key, flow.value)
	            builder.applicationContext = event.ctx

	            FlowAssembler flowAssembler = new FlowAssembler(flow.key, builder)
	            flowRegistry.registerFlowDefinition(new StaticFlowDefinitionHolder(flowAssembler.assembleFlow()))
	        }			
		}
    }
}