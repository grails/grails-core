/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.webflow.engine.builder

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry
import org.springframework.webflow.definition.registry.FlowDefinitionRegistryImpl
import org.springframework.webflow.engine.builder.DefaultFlowHolder
import org.springframework.webflow.engine.builder.FlowAssembler
import org.springframework.webflow.engine.builder.support.FlowBuilderServices
import org.codehaus.groovy.grails.commons.GrailsControllerClass

/**
* A flow execution repository that scans the set GrailsApplication instance for controllers
* that contain flow closures
*
* @author Graeme Rocher
* @since 0.6
 *
* Created: Jul 3, 2007
* Time: 8:13:08 AM
*
*/

class ControllerFlowRegistry implements GrailsApplicationAware, ApplicationContextAware, FactoryBean, InitializingBean {


    ApplicationContext applicationContext
    GrailsApplication grailsApplication
    FlowDefinitionRegistry flowRegistry
    FlowBuilderServices flowBuilderServices

    /**
     * Implements the doPopulate method by using a GrailsApplication instance and its contained
     * controller classes to locate and populate flow definitions in the registry
     */
    protected void doPopulate(FlowDefinitionRegistry registry) {
        if(grailsApplication) {
            for(GrailsControllerClass c in grailsApplication.controllerClasses) {
                for(flow in c.flows) {
                    def flowId = ("${c.logicalPropertyName}/" + flow.key).toString()

                    FlowBuilder builder = new FlowBuilder( flowId, flow.value,flowBuilderServices, registry)
                    builder.viewPath = "/"
                    builder.applicationContext = applicationContext
                    FlowAssembler assembler = new FlowAssembler(builder, builder.getFlowBuilderContext())
                    registry.registerFlowDefinition new DefaultFlowHolder(assembler)
                }
            }

        }

    }

    Object getObject() { flowRegistry }

    Class getObjectType() { FlowDefinitionRegistry }

    boolean isSingleton() { true }


    public void afterPropertiesSet() {
        flowRegistry = new FlowDefinitionRegistryImpl()
        doPopulate flowRegistry
    }
}
