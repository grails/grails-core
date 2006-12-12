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

import org.codehaus.groovy.grails.plugins.support.*
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
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;


/**
 * A plug-in that handles the configuration of controllers for Grails 
 * 
 * @author Graeme Rocher
 * @since 0.4
 */
class ControllersGrailsPlugin {

	def watchedResources = ["**/grails-app/controllers/*Controller.groovy",
	                        "**/grails-app/taglib/*TagLib.groovy"]
	
	def version = GrailsPluginUtils.getGrailsVersion()
	def dependsOn = [i18n:version]
	
	def doWithSpring = {
		exceptionHandler(GrailsExceptionResolver) {
			exceptionMappings = ['java.lang.Exception':'/error']
		}
		multipartResolver(CommonsMultipartResolver)
		def urlMappings = [:]
		grailsUrlMappings(UrlMappingFactoryBean) {
			mappings = urlMappings
		}
		simpleGrailsController(SimpleGrailsController.class) {
			grailsApplication = ref("grailsApplication", true)			
		}
		
	    
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
					
		}
		
		// Go through all the controllers and configure them in spring with AOP proxies for auto-updates and
		// mappings in the urlMappings bean
		application.controllers.each { controller ->
			log.debug "Configuring controller $controller.fullName"
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
		def controllers = [] as HashSet
		def webflows = []
		def basedir = System.getProperty("base.dir")
		def grailsEnv = System.getProperty("grails.env")
		
		// first for all the watched resources for this controller that are controllers
		// create a servlet-mapping element that maps to the Grails dispatch servlet
	    plugin.watchedResources.each {
	        def match = it.filename =~ /(\w+)(Controller.groovy$)/
	        if(match) {
	            def controllerName = match[0][1]
	            controllerName = GCU.getPropertyName(controllerName)	            
	            controllers << controllerName
	        }
		}
		def mappingElement = webXml.'servlet-mapping'		
		controllers.each { c ->
			mappingElement + {
				'servlet-mapping' {
					'servlet-name'("grails")
					'url-pattern'("/${c}/*")
				}
			}
		}
		
		if(grailsEnv == "development") {
			// if we're in development environment first add a the reload filter
			// to the web.xml by finding the last filter and appending it after
			def lastFilter = webXml.filter

			def reloadFilter = 'reloadFilter'			                               
			lastFilter[lastFilter.size()-1] + {
				filter {
					'filter-name'(reloadFilter)
					'filter-class'('org.codehaus.groovy.grails.web.servlet.filter.GrailsReloadServletFilter')
				}
			}
			// now map each controller request to the filter
			def lastFilterMapping = webXml.'filter-mapping'
			                                                
			controllers.each { c ->
				lastFilterMapping[lastFilterMapping.size()-1] + {
					'filter-mapping' {						
						'filter-name'(reloadFilter)
						'url-pattern'("/${c}/*")						
					}
				}			
			}
			// now find the GSP servlet and allow viewing generated source in
			// development mode
			def gspServlet = webXml.servlet.find { it.'servlet-name'?.text() == 'gsp' }
			gspServlet.'servlet-class' + {
				'init-param' {
					description """
		              Allows developers to view the intermediade source code, when they pass
		                a spillGroovy argument in the URL.					
							"""					
					'param-name'('showSource')
					'param-value'(1)
				}
			}
		}

	}
	
	def onChange = { event ->
		if(GCU.isControllerClass(event.source)) {
			log.debug("Controller ${event.source} changed. Reloading...")
			def context = event.ctx
			if(!context) {
				log.debug("Application context not found. Can't reload")
				return
			}
			boolean isNew = application.getController(event.source?.name) ? false : true
										
			def controllerClass = application.addControllerClass(event.source)
			
			def mappings = new Properties()
			application.controllers.each { c ->
				c.URIs.each { uri ->
				  mappings[uri] = SimpleGrailsController.APPLICATION_CONTEXT_ID
				}
			}
			
			def urlMappingsTargetSource = context.getBean(GrailsUrlHandlerMapping.APPLICATION_CONTEXT_TARGET_SOURCE)
			def urlMappings = new GrailsUrlHandlerMapping(applicationContext:context)
			urlMappings.mappings = mappings
			
            def interceptorNames = context.getBeanNamesForType(HandlerInterceptor.class)
            def webRequestInterceptors = context.getBeanNamesForType( WebRequestInterceptor.class)			
		
            HandlerInterceptor[] interceptors = new HandlerInterceptor[interceptorNames.size()+webRequestInterceptors.size()]
                
			def j = 0                                                                       
			for(i in 0..<interceptorNames.size()) {
				interceptors[i] = context.getBean(interceptorNames[i])
				j = i+1
			}
			for(i in 0..<webRequestInterceptors.size()) {
				j = i+j
				interceptors[j] = new WebRequestHandlerInterceptorAdapter(context.getBean(webRequestInterceptors[i]))
			}
         
			log.debug("Re-adding ${interceptors.length} interceptors to mapping")
			
			urlMappings.interceptors = interceptors
			urlMappings.initApplicationContext()
			
			urlMappingsTargetSource.swap(urlMappings)
			
			def controllerTargetSource = context.getBean("${controllerClass.fullName}TargetSource")
			controllerTargetSource.swap(controllerClass)
			
			if(isNew) {
				log.info "Re-generating web.xml file..."
				def webTemplateXml = resolver.getResource("/WEB-INF/web.template.xml")
				def webXml = resolver.getResource("/WEB-INF/web.xml")?.getFile()
				webXml?.withWriter { w ->
					manager.doWebDescriptor(webTemplateXml, w)
				}				
			}
			
		}
		else if(GCU.isTagLibClass(event.source)) {
			boolean isNew = application.getGrailsTagLibClass(event.source?.name) ? false : true
			def taglibClass = application.addTagLibClass(event.source)
			if(taglibClass) {
				if(isNew) {
					GrailsRuntimeConfigurator.registerTagLibrary(taglibClass, source.ctx)
				}
				else {
					def targetSource = event.ctx?.getBean("${taglibClass.fullName}TargetSource")
					targetSource?.swap(taglibClass)
				}
			}
		}
	}
}