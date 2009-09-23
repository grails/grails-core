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

import grails.util.BuildSettingsHolder
import grails.util.Environment
import grails.util.PluginBuildSettings

import groovy.lang.MetaClass
import java.lang.reflect.Modifier

import org.codehaus.groovy.grails.commons.GrailsClass
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsTagLibClass
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler
import org.codehaus.groovy.grails.plugins.PluginMetaManager
import org.codehaus.groovy.grails.web.pages.ext.jsp.TagLibraryResolver
import org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.view.GrailsViewResolver
import org.springframework.beans.factory.config.PropertiesFactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.core.io.FileSystemResource
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.plugins.web.taglib.*
import org.codehaus.groovy.grails.web.pages.*

/**
 * A Plugin that sets up and configures the GSP and GSP tag library support in Grails 
 *
 * @author Graeme Rocher
 * @since 1.1
 * 
 * Created: Jan 5, 2009
 */

public class GroovyPagesGrailsPlugin {

    // monitor all resources that end with TagLib.groovy
    def watchedResources = [ "file:./plugins/*/grails-app/taglib/**/*TagLib.groovy",
                             "file:./grails-app/taglib/**/*TagLib.groovy"]


    def version = grails.util.GrailsUtil.getGrailsVersion()
    def dependsOn = [core: version, i18n: version]
    def observe = ['controllers']


    // Provide these tag libraries declaratively
    def providedArtefacts = [
            ApplicationTagLib,
            CountryTagLib,
            FormatTagLib,
            FormTagLib,
            JavascriptTagLib,
            RenderTagLib,
            ValidationTagLib,
            PluginTagLib
    ]



    /**
     * Clear the page cache with the ApplicationContext is loaded
     */
    def doWithApplicationContext = {ApplicationContext ctx ->
        GroovyPagesTemplateEngine templateEngine = ctx.getBean("groovyPagesTemplateEngine")
        templateEngine.clearPageCache()
    }

    /**
     * Configures the various Spring beans required by GSP
     */
    def doWithSpring = {
        // A bean used to resolve JSP tag libraries
        jspTagLibraryResolver(TagLibraryResolver)
        // A bean used to resolve GSP tag libraries
        gspTagLibraryLookup(TagLibraryLookup)

        boolean developmentMode = !application.warDeployed
        Environment env = Environment.current
        boolean enableReload = env.isReloadEnabled() || application.config.grails.gsp.enable.reload == true || (developmentMode && env == Environment.DEVELOPMENT)
        boolean warDeployedWithReload = application.warDeployed && enableReload

        // If the development environment is used we need to load GSP files relative to the base directory
        // as oppose to in WAR deployment where views are loaded from /WEB-INF
        def viewsDir = application.config.grails.gsp.view.dir
        if (viewsDir) {
            log.info "Configuring GSP views directory as '${viewsDir}'"
            groovyPageResourceLoader(org.codehaus.groovy.grails.web.pages.GroovyPageResourceLoader) {
                baseResource = "file:${viewsDir}"
                pluginSettings = createPluginSettings()
            }
        }
        else {
            if (developmentMode) {
                groovyPageResourceLoader(org.codehaus.groovy.grails.web.pages.GroovyPageResourceLoader) {
                    baseResource = new FileSystemResource(".")
                pluginSettings = createPluginSettings()
                }
            }
            else {
                if (warDeployedWithReload) {
                    groovyPageResourceLoader(org.codehaus.groovy.grails.web.pages.GroovyPageResourceLoader) {
                        if(env.hasReloadLocation()) {
                            def location = env.reloadLocation
                            if(!location.endsWith(File.separator)) location = "${location}${File.separator}"
                            baseResource = "file:${location}"
                        }
                        else {
                            baseResource = "/WEB-INF"
                        }
                        pluginSettings = createPluginSettings()
                    }
                }
            }
        }

        // Setup the main templateEngine used to render GSPs
        groovyPagesTemplateEngine(org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine) {
            classLoader = ref("classLoader")
            if (developmentMode || warDeployedWithReload) {
                resourceLoader = groovyPageResourceLoader
            }
            if (enableReload) {
                reloadEnabled = enableReload
            }
            tagLibraryLookup = gspTagLibraryLookup
            jspTagLibraryResolver = jspTagLibraryResolver
			precompiledGspMap = { PropertiesFactoryBean pfb -> 
				ignoreResourceNotFound = true
				location = "classpath:gsp/views.properties"
			}
        }
        
        // Configure a Spring MVC view resolver
        jspViewResolver(GrailsViewResolver) {
            viewClass = org.springframework.web.servlet.view.JstlView.class
            prefix = GrailsApplicationAttributes.PATH_TO_VIEWS
            suffix = ".jsp"
            templateEngine = groovyPagesTemplateEngine
            pluginMetaManager = ref("pluginMetaManager", true)
            if (developmentMode) {
                resourceLoader = groovyPageResourceLoader
            }
        }

        // Now go through tag libraries and configure them in spring too. With AOP proxies and so on
        for(taglib in application.tagLibClasses) {
            "${taglib.fullName}"(taglib.clazz) { bean ->
                   bean.autowire = true
                   // Taglib scoping support could be easily added here. Scope could be based on a static field in the taglib class.
				   //bean.scope = 'request'
            }
        }

    }

    /**
     * Modifies the web.xml when in development mode to allow viewing of sources
     */
    def doWithWebDescriptor = { webXml ->
        if (Environment.current == Environment.DEVELOPMENT) {
            // Find the GSP servlet and allow viewing generated source in
            // development mode
            def gspServlet = webXml.servlet.find {it.'servlet-name'?.text() == 'gsp'}
            gspServlet.'servlet-class' + {
                'init-param'
                {
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


    /**
     * Sets up dynamic methods required by the GSP implementation including dynamic tag method dispatch
     */
    def doWithDynamicMethods = { ApplicationContext ctx ->

    	WebMetaUtils.registerStreamCharBufferMetaClass()

        TagLibraryLookup gspTagLibraryLookup = ctx.getBean("gspTagLibraryLookup")

        if(manager?.hasGrailsPlugin("controllers")) {
            for(namespace in gspTagLibraryLookup.availableNamespaces) {
                def propName = GrailsClassUtils.getGetterName(namespace)
                def namespaceDispatcher = gspTagLibraryLookup.lookupNamespaceDispatcher(namespace)
                def controllerClasses = application.controllerClasses*.clazz
                for(Class controllerClass in controllerClasses) {
                    MetaClass mc = controllerClass.metaClass
                    if(!mc.getMetaProperty(namespace)) {
                        mc."$propName" = { namespaceDispatcher }                
                    }
                    registerControllerMethodMissing(mc, gspTagLibraryLookup, ctx)
                    Class superClass = controllerClass.superclass
                    // deal with abstract super classes
                    while (superClass != Object.class) {
                        if (Modifier.isAbstract(superClass.getModifiers())) {
                            registerControllerMethodMissing(superClass.metaClass, gspTagLibraryLookup, ctx)
                        }
                        superClass = superClass.superclass
                    }

                }
            }
        }

        for (GrailsTagLibClass t in application.tagLibClasses) {
            GrailsTagLibClass taglib = t
            MetaClass mc = taglib.metaClass
            String namespace = taglib.namespace ?: GroovyPage.DEFAULT_NAMESPACE

            WebMetaUtils.registerCommonWebProperties(mc, application)

            for(tag in taglib.tagNames) {
                WebMetaUtils.registerMethodMissingForTags(mc, gspTagLibraryLookup, namespace, tag)
            }

            mc.getTagNamesThatReturnObject = {->
            	taglib.getTagNamesThatReturnObject()
            }
            
            mc.throwTagError = {String message ->
                throw new org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException(message)
            }
            mc.getPluginContextPath = {->
                PluginMetaManager metaManager = ctx.pluginMetaManager
                String path = metaManager.getPluginPathForResource(delegate.class.name)
                path ? path : ""
            }

            mc.getPageScope = {->
                def request = RequestContextHolder.currentRequestAttributes().currentRequest
                def binding = request.getAttribute(GrailsApplicationAttributes.PAGE_SCOPE)
                if (!binding) {
                    binding = new GroovyPageBinding()
                    request.setAttribute(GrailsApplicationAttributes.PAGE_SCOPE, binding)
                }
                binding
            }

            mc.getOut = {->
            	GroovyPageOutputStack.currentWriter()
            }
            mc.setOut = {Writer newOut ->
            	GroovyPageOutputStack.currentStack().push(newOut,true)
            }
            mc.propertyMissing = {String name ->
                def result = gspTagLibraryLookup.lookupNamespaceDispatcher(name)
                if(result == null) {
                    def tagLibrary = gspTagLibraryLookup.lookupTagLibrary(namespace, name)
                    if(!tagLibrary) tagLibrary = gspTagLibraryLookup.lookupTagLibrary(GroovyPage.DEFAULT_NAMESPACE, name)

                    def tagProperty = tagLibrary?."$name"
                    result = tagProperty ? tagProperty.clone() : null
                }

                if(result!=null) {
                    mc."${GrailsClassUtils.getGetterName(name)}" = {-> result}
                    return result
                }
                else {
                    throw new MissingPropertyException(name, delegate.class)
                }

            }
            mc.methodMissing = {String name, args ->
            	def usednamespace = namespace
                def tagLibrary = gspTagLibraryLookup.lookupTagLibrary(namespace, name)
                if(!tagLibrary) {
                	tagLibrary = gspTagLibraryLookup.lookupTagLibrary(GroovyPage.DEFAULT_NAMESPACE, name)
					usednamespace = GroovyPage.DEFAULT_NAMESPACE
                }
                if(tagLibrary) {
                	WebMetaUtils.registerMethodMissingForTags(mc, gspTagLibraryLookup, usednamespace, name)
                    //WebMetaUtils.registerMethodMissingForTags(mc, tagLibrary, name)
                }
                if (mc.respondsTo(delegate, name, args)) {
                    return mc.invokeMethod(delegate, name, args)
                }
                else {
                    throw new MissingMethodException(name, delegate.class, args)
                }
            }
            ctx.getBean(taglib.fullName).metaClass = mc
        }

    }



    def registerControllerMethodMissing(MetaClass mc, TagLibraryLookup lookup, ApplicationContext ctx) {
        // allow controllers to call tag library methods
        mc.methodMissing = {String name, args ->
            args = args == null ? [] as Object[] : args            
            def tagLibrary = lookup.lookupTagLibrary(GroovyPage.DEFAULT_NAMESPACE, name)
            if (tagLibrary) {
                MetaClass controllerMc = delegate.class.metaClass
                WebMetaUtils.registerMethodMissingForTags(controllerMc, lookup, GroovyPage.DEFAULT_NAMESPACE, name)
                if(controllerMc.respondsTo(delegate, name, args)) {
                  return controllerMc.invokeMethod(delegate, name, args)
                }
                else {
                  throw new MissingMethodException(name, delegate.class, args)
                }
            }
            else {
                throw new MissingMethodException(name, delegate.class, args)
            }
        }

    }

    def onChange = { event ->
        if (application.isArtefactOfType(TagLibArtefactHandler.TYPE, event.source)) {

            GrailsClass taglibClass = application.addArtefact(TagLibArtefactHandler.TYPE, event.source)
            if (taglibClass) {
                // replace tag library bean
                def beanName = taglibClass.fullName
                def beans = beans {
                    "$beanName"(taglibClass.getClazz()) {bean ->
                        bean.autowire = true
						//bean.scope = 'request'
                    }
                }
                beans.registerBeans(event.ctx)

                // The tag library lookup class caches "tag -> taglib class"
                // so we need to update it now.
                def lookup = event.ctx.getBean("gspTagLibraryLookup")
                lookup.registerTagLib(taglibClass)
            }
        }

        event.manager?.getGrailsPlugin("groovyPages")?.doWithDynamicMethods(event.ctx)

    }

    private PluginBuildSettings createPluginSettings() {
        return new PluginBuildSettings(BuildSettingsHolder.settings);
    }
}
