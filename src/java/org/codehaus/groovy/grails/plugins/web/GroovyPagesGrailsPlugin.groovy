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


import org.springframework.web.context.request.RequestContextHolder as RCH
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU

import org.codehaus.groovy.grails.plugins.web.taglib.*
import org.springframework.context.ApplicationContext
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.codehaus.groovy.grails.web.pages.ext.jsp.TagLibraryResolver
import org.codehaus.groovy.grails.web.pages.TagLibraryLookup
import org.springframework.core.io.FileSystemResource
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import grails.util.Environment
import org.codehaus.groovy.grails.commons.GrailsTagLibClass
import org.codehaus.groovy.grails.plugins.PluginMetaManager
import org.codehaus.groovy.grails.web.pages.GroovyPage
import org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsClass
import org.codehaus.groovy.grails.web.taglib.NamespacedTagDispatcher
import org.codehaus.groovy.grails.web.servlet.view.GrailsViewResolver
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.commons.GrailsApplication
import java.lang.reflect.Modifier
import org.springframework.beans.factory.config.PropertiesFactoryBean

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
            }
        }
        else {
            if (developmentMode) {
                groovyPageResourceLoader(org.codehaus.groovy.grails.web.pages.GroovyPageResourceLoader) {
                    baseResource = new FileSystemResource(".")
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
                def tagLibrary = gspTagLibraryLookup.lookupTagLibrary(namespace, tag)
                WebMetaUtils.registerMethodMissingForTags(mc, tagLibrary, tag)
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
                def binding = request[GrailsApplicationAttributes.PAGE_SCOPE]
                if (!binding) {
                    binding = new Binding()
                    request[GrailsApplicationAttributes.PAGE_SCOPE] = binding
                }
                binding
            }

            mc.getOut = {->
                RequestContextHolder.currentRequestAttributes().out
            }
            mc.setOut = {Writer newOut ->
                RequestContextHolder.currentRequestAttributes().out = newOut
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
                def tagLibrary = gspTagLibraryLookup.lookupTagLibrary(namespace, name)
                if(!tagLibrary) tagLibrary = gspTagLibraryLookup.lookupTagLibrary(GroovyPage.DEFAULT_NAMESPACE, name)
                if(tagLibrary) {
                    WebMetaUtils.registerMethodMissingForTags(mc, tagLibrary, name)
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
                WebMetaUtils.registerMethodMissingForTags(controllerMc,tagLibrary, name)
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
}