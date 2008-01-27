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

import org.codehaus.groovy.grails.plugins.support.GrailsPluginUtils
import org.codehaus.groovy.grails.commons.metaclass.*
import org.codehaus.groovy.grails.support.ClassEditor
import org.springframework.beans.factory.config.CustomEditorConfigurer
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.grails.commons.cfg.GrailsOverrideConfigurer
import org.codehaus.groovy.grails.commons.cfg.GrailsPlaceholderConfigurer
import org.springframework.core.io.Resource
import org.codehaus.groovy.grails.commons.spring.DefaultRuntimeSpringConfiguration
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator
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
        addBeanFactoryPostProcessor(new GrailsOverrideConfigurer())
        addBeanFactoryPostProcessor(new GrailsPlaceholderConfigurer())


        classLoader(MethodInvokingFactoryBean) {
			targetObject = ref("grailsApplication", true)
			targetMethod = "getClassLoader"
		}
		classEditor(ClassEditor) {
			classLoader = classLoader
		}
		customEditors(CustomEditorConfigurer) {
			customEditors = [(java.lang.Class.class):classEditor]
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