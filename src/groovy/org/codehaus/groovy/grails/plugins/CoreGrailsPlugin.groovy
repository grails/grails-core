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

import org.codehaus.groovy.grails.plugins.support.*
import org.codehaus.groovy.grails.commons.metaclass.*
import org.codehaus.groovy.grails.support.ClassEditor
import org.springframework.beans.factory.config.CustomEditorConfigurer
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * A plug-in that configures the core shared beans within the Grails application context 
 * 
 * @author Graeme Rocher
 * @since 0.4
 */
class CoreGrailsPlugin {
	
	def version = GrailsPluginUtils.getGrailsVersion()
	
	def doWithSpring = {
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
		MetaClassRegistry registry = InvokerHelper
										.getInstance()
										.getMetaRegistry();

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
				return registry.getMetaClass(delegate)
			}
		}
	}
}