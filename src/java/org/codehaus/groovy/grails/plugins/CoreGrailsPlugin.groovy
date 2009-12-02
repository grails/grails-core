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
package org.codehaus.groovy.grails.plugins

import org.codehaus.groovy.grails.commons.metaclass.*
import org.codehaus.groovy.grails.support.ClassEditor
import org.springframework.beans.factory.config.CustomEditorConfigurer
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.codehaus.groovy.grails.commons.cfg.MapBasedSmartPropertyOverrideConfigurer
import org.codehaus.groovy.grails.commons.cfg.GrailsPlaceholderConfigurer
import org.springframework.core.io.Resource
import org.codehaus.groovy.grails.commons.spring.DefaultRuntimeSpringConfiguration
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAwareBeanPostProcessor
import org.codehaus.groovy.grails.support.DevelopmentShutdownHook
import grails.util.Environment
import grails.util.Metadata;

import org.codehaus.groovy.grails.aop.framework.autoproxy.GroovyAwareInfrastructureAdvisorAutoProxyCreator
import org.codehaus.groovy.grails.plugins.support.aware.PluginManagerAwareBeanPostProcessor

/**
 * A plug-in that configures the core shared beans within the Grails application context 
 * 
 * @author Graeme Rocher
 * @since 0.4
 */
class CoreGrailsPlugin {
	
	def version = grails.util.GrailsUtil.getGrailsVersion()
    def watchedResources = ["file:./grails-app/conf/spring/resources.xml","file:./grails-app/conf/spring/resources.groovy"]
	
	
	def doWithSpring = {
        xmlns context:"http://www.springframework.org/schema/context"
        xmlns grailsContext:"http://grails.org/schema/context"


        addBeanFactoryPostProcessor(new MapBasedSmartPropertyOverrideConfigurer(application.config.beans, application.classLoader))
        addBeanFactoryPostProcessor(new GrailsPlaceholderConfigurer())

        // replace AutoProxy advisor with Groovy aware one
        "org.springframework.aop.config.internalAutoProxyCreator"(GroovyAwareInfrastructureAdvisorAutoProxyCreator)

        // Allow the use of Spring annotated components
        context.'annotation-config'()
        def beanPackages = application.config.grails.spring.bean.packages
        if(beanPackages instanceof List) {
            grailsContext.'component-scan'('base-package':beanPackages.join(','))
        }

        grailsApplicationPostProcessor(GrailsApplicationAwareBeanPostProcessor, ref("grailsApplication", true))
        if(getParentCtx()?.containsBean('pluginManager'))
            pluginManagerPostProcessor(PluginManagerAwareBeanPostProcessor, ref('pluginManager', true))

        classLoader(MethodInvokingFactoryBean) {
			targetObject = ref("grailsApplication", true)
			targetMethod = "getClassLoader"
		}

        // add shutdown hook if not running in war deployed mode
        if(!Metadata.getCurrent().isWarDeployed() || Environment.currentEnvironment == Environment.DEVELOPMENT)
            shutdownHook(DevelopmentShutdownHook)
        
		customEditors(CustomEditorConfigurer) {
			customEditors = [(java.lang.Class.name):ClassEditor.name]
		}
	}
	
	def doWithDynamicMethods = {
		MetaClassRegistry registry = GroovySystem.metaClassRegistry

		def metaClass = registry.getMetaClass(Class.class)
		if(!(metaClass instanceof ExpandoMetaClass)) {
			registry.removeMetaClass(Class.class)
			metaClass = registry.getMetaClass(Class.class)
		}

		metaClass.getMetaClass = {->
			def mc = registry.getMetaClass(delegate)
			if(mc instanceof ExpandoMetaClass) {
				return mc
			}
			else {
 
				registry.removeMetaClass(delegate)
				if(registry.metaClassCreationHandler instanceof ExpandoMetaClassCreationHandle)				
					return registry.getMetaClass(delegate)
			   	else {
				 	def emc = new ExpandoMetaClass(delegate, false, true)
					emc.initialize()
					registry.setMetaClass(delegate, emc)    
					return emc
				}					
			}
		}
	}
	
	def onChange = { event ->
	    println "Change event: $event"
		if(event.source instanceof Resource) {
			def xmlBeans = new org.springframework.beans.factory.xml.XmlBeanFactory(event.source);
            xmlBeans.beanDefinitionNames.each { name ->                         
	        	event.ctx.registerBeanDefinition(name, xmlBeans.getBeanDefinition(name))
			}
		} else if (event.source instanceof Class) {
            println "Change event was class... reloading spring resources.groovy beans"
            RuntimeSpringConfiguration springConfig = event.ctx != null ? new DefaultRuntimeSpringConfiguration(event.ctx) : new DefaultRuntimeSpringConfiguration();
            GrailsRuntimeConfigurator.loadSpringGroovyResourcesIntoContext(springConfig, application.classLoader, event.ctx)
        }
	}
}