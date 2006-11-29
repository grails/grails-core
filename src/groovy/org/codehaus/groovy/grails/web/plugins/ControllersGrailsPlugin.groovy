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
package org.codehaus.groovy.grails.web.plugins;

import org.codehaus.groovy.grails.web.errors.GrailsExceptionResolver;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsUrlHandlerMapping;
import org.codehaus.groovy.grails.web.servlet.mvc.SimpleGrailsController;
import org.codehaus.groovy.grails.web.servlet.view.GrailsViewResolver;
import org.codehaus.groovy.grails.beans.factory.UrlMappingFactoryBean;
import org.springframework.aop.target.HotSwappableTargetSource;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.codehaus.groovy.grails.commons.spring.*
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;

// TODO move to hibernate plugin
import org.springframework.orm.hibernate3.support.OpenSessionInViewInterceptor;
import org.springframework.orm.hibernate3.HibernateAccessor;


/**
 * A plug-in that handles the configuration of controllers for Grails 
 * 
 * @author Graeme Rocher
 * @since 0.4
 */
class ControllersGrailsPlugin {

	def watchedResources = "**/grails-app/controllers/*Controller.groovy"
	
	def version = 1.0
	
	def doWithSpring = {
		exceptionHandler(GrailsExceptionResolver) {
			exceptionMappings = ['java.lang.Exception':'/error']
		}
		multipartResolver(CommonsMultipartResolver)
		def urlMappings = [:]
		grailsUrlMappings(UrlMappingFactoryBean) {
			mappings = urlMappings
		}
		def simpleController = simpleGrailsController(SimpleGrailsController.class)
		simpleController.autowire = "byType"
	    
		jspViewResolver(GrailsViewResolver) {
			viewClass = org.springframework.web.servlet.view.JstlView.class
			prefix = GrailsApplicationAttributes.PATH_TO_VIEWS
		    suffix = ".jsp"
		}
		if(application.controllers) {
			def handlerInterceptors = []
				                           
			grailsUrlHandlerMapping(GrailsUrlHandlerMapping) {
				interceptors = handlerInterceptors
				mappings =  grailsUrlMappings				                
			}
			handlerMappingTargetSource(HotSwappableTargetSource, grailsUrlHandlerMapping)
			handlerMapping(ProxyFactoryBean) {
				targetSource = handlerMappingTargetSource
				proxyInterfaces = [org.springframework.web.servlet.HandlerMapping]
			}
						
			// TODO move to hibernate plugin
			if(application.grailsDomainClasses.size() > 0) {
				openSessionInViewInterceptor(OpenSessionInViewInterceptor) {
					flushMode = HibernateAccessor.FLUSH_AUTO
					sessionFactory = sessionFactory
				}	
				handlerInterceptors << openSessionInViewInterceptor
			}
		}
		
		// Go through all the controllers and configure them in spring with AOP proxies for auto-updates and
		// mappings in the urlMappings bean
		application.controllers.each { controller ->
			if(controller.available) {
				configureAOPProxyBean.delegate = delegate
				configureAOPProxyBean(controller, "getController", org.codehaus.groovy.grails.commons.GrailsControllerClass.class)				
				controller.URIs.each { uri ->
					if(!urlMappings.containsKey(uri)) 
						urlMappings[uri] = "simpleGrailsController"
				}
			}
		}
		
		// Now go through tag libraries and configure them in spring too. With AOP proxies and so on
		application.grailsTabLibClasses.each { taglib ->
			configureAOPProxyBean.delegate = delegate
			configureAOPProxyBean(taglib, "getGrailsTagLibClass", org.codehaus.groovy.grails.commons.GrailsTagLibClass.class)
		}
	}
	
	def configureAOPProxyBean = { grailsClass, factoryMethod, proxyClass ->
		"${grailsClass.fullName}Class"(MethodInvokingFactoryBean) {
			targetObject = ref("grailsApplication", true)
			targetMethod = factoryMethod
			arguments = [grailsClass.fullName]
		}
		"${grailsClass.fullName}TargetSource"(HotSwappableTargetSource, ref("${grailsClass.fullName}Class"))
		
		"${grailsClass.fullName}Proxy"(ProxyFactoryBean) {
			targetSource = ref("${grailsClass.fullName}TargetSource")
			proxyInterfaces = [proxyClass]
		}
		"${grailsClass.fullName}"("${grailsClass.fullName}Proxy":"newInstance") { bean ->			
			bean.singleton = false
			bean.autowire = "byName"
		}		
			
	}
	
	def doWithWebDescriptor = { webXml ->
		def controllers = []
		def webflows = []
		def basedir = System.getProperty("base.dir")
		
	    watchedResources.each {
	        def match = it.filename =~ /(\w+)(Controller.groovy$)/
	        if(match) {
	            def controllerName = match[0][1]
	            controllerName = GCU.getPropertyName(controllerName)
	            controllers << controllerName
	        }
		}
		
		def mappingElement = webXml.find { it.name() == 'servlet-mapping' }		
		controllers.each {
			def el = mappingElement.appendSibling('servlet-mapping')
			el.'servlet-name'.value = "grails"
			el.'url-pattern'.value = "/${it}/*"
		}

	}
	
	def onChange = { event ->
			 
	}
}