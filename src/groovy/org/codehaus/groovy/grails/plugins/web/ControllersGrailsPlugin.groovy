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
package org.codehaus.groovy.grails.plugins.web;

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.springframework.web.context.request.RequestContextHolder as RCH

import grails.util.GrailsUtil
import java.lang.reflect.Modifier
import org.codehaus.groovy.grails.beans.factory.UrlMappingFactoryBean
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.plugins.PluginMetaManager
import org.codehaus.groovy.grails.plugins.web.taglib.*
import org.codehaus.groovy.grails.validation.ConstrainedPropertyBuilder
import org.codehaus.groovy.grails.web.errors.GrailsExceptionResolver
import org.codehaus.groovy.grails.web.metaclass.*
import org.codehaus.groovy.grails.web.pages.GroovyPage
import org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsUrlHandlerMapping
import org.codehaus.groovy.grails.web.servlet.mvc.SimpleGrailsController
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.ControllerExecutionException
import org.codehaus.groovy.grails.web.servlet.view.GrailsViewResolver
import org.codehaus.groovy.grails.web.taglib.NamespacedTagDispatcher
import org.springframework.aop.framework.ProxyFactoryBean
import org.springframework.aop.target.HotSwappableTargetSource
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.core.io.FileSystemResource
import org.springframework.validation.BindException
import org.springframework.validation.Errors
import org.springframework.web.servlet.ModelAndView
import org.codehaus.groovy.grails.web.multipart.ContentLengthAwareCommonsMultipartResolver
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.codehaus.groovy.grails.commons.metaclass.LazyMetaPropertyMap
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine

/**
* A plug-in that handles the configuration of controllers for Grails
*
* @author Graeme Rocher
* @since 0.4
*/
class ControllersGrailsPlugin {

    def watchedResources = ["file:./grails-app/controllers/**/*Controller.groovy",
            "file:./plugins/*/grails-app/controllers/**/*Controller.groovy",
            "file:./plugins/*/grails-app/taglib/**/*TagLib.groovy",
            "file:./grails-app/taglib/**/*TagLib.groovy"]

    def version = grails.util.GrailsUtil.getGrailsVersion()
    def dependsOn = [core: version, i18n: version]
    def providedArtefacts = [ApplicationTagLib,
            FormatTagLib,
            FormTagLib,
            JavascriptTagLib,
            RenderTagLib,
            ValidationTagLib
            ]

    def doWithApplicationContext = { ApplicationContext ctx ->
        GroovyPagesTemplateEngine templateEngine = ctx.getBean("groovyPagesTemplateEngine")
        templateEngine.clearPageCache()
    }
    
    def doWithSpring = {
        exceptionHandler(GrailsExceptionResolver) {
            exceptionMappings = ['java.lang.Exception': '/error']
        }
        multipartResolver(ContentLengthAwareCommonsMultipartResolver)
        def urlMappings = [:]
        grailsUrlMappings(UrlMappingFactoryBean) {
            mappings = urlMappings
        }
        simpleGrailsController(SimpleGrailsController.class) {
            grailsApplication = ref("grailsApplication", true)
        }

        boolean developmentMode = !application.warDeployed


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
        }


        groovyPagesTemplateEngine(org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine) {
            classLoader = ref("classLoader")
            if (developmentMode) {
                resourceLoader = groovyPageResourceLoader
            }
            if (grails.util.GrailsUtil.isDevelopmentEnv() || application.config.grails.gsp.enable.reload == true) {
                reloadEnabled = true
            }
        }

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


        def handlerInterceptors = [ref("localeChangeInterceptor")]
        grailsUrlHandlerMapping(GrailsUrlHandlerMapping) {
            interceptors = handlerInterceptors
            mappings = grailsUrlMappings
        }
        handlerMappingTargetSource(HotSwappableTargetSource, grailsUrlHandlerMapping)
        handlerMapping(ProxyFactoryBean) {
            targetSource = handlerMappingTargetSource
            proxyInterfaces = [org.springframework.web.servlet.HandlerMapping]
        }

        application.controllerClasses.each {controller ->
            log.debug "Configuring controller $controller.fullName"
            if (controller.available) {
                // configure the controller with AOP proxies for auto-updates and
                // mappings in the urlMappings bean
                configureAOPProxyBean.delegate = delegate
                configureAOPProxyBean(controller, ControllerArtefactHandler.TYPE, org.codehaus.groovy.grails.commons.GrailsControllerClass.class, false)
            }

        }

        // Now go through tag libraries and configure them in spring too. With AOP proxies and so on
        application.tagLibClasses.each {taglib ->
            configureAOPProxyBean.delegate = delegate
            configureAOPProxyBean(taglib, TagLibArtefactHandler.TYPE, org.codehaus.groovy.grails.commons.GrailsTagLibClass.class, true)
        }
    }

    def configureAOPProxyBean = {grailsClass, artefactType, proxyClass, singleton ->
        "${grailsClass.fullName}Class"(MethodInvokingFactoryBean) {
            targetObject = ref("grailsApplication", true)
            targetMethod = "getArtefact"
            arguments = [artefactType, grailsClass.fullName]
        }
        "${grailsClass.fullName}TargetSource"(HotSwappableTargetSource, ref("${grailsClass.fullName}Class"))

        "${grailsClass.fullName}Proxy"(ProxyFactoryBean) {
            targetSource = ref("${grailsClass.fullName}TargetSource")
            proxyInterfaces = [proxyClass]
        }
        "${grailsClass.fullName}"("${grailsClass.fullName}Proxy": "newInstance") {bean ->
            bean.singleton = singleton
            bean.autowire = "byName"
        }

    }

    def doWithWebDescriptor = {webXml ->

        def basedir = System.getProperty("base.dir")
        def grailsEnv = GrailsUtil.getEnvironment()

        def mappingElement = webXml.'servlet-mapping'
        mappingElement = mappingElement[mappingElement.size()-1]
        
        mappingElement + {
            'servlet-mapping' {
                'servlet-name'("grails")
                'url-pattern'("*.dispatch")
            }
        }

        def filters = webXml.filter
        def filterMappings = webXml.'filter-mapping'

        def lastFilter = filters[filters.size() - 1]
        def lastFilterMapping = filterMappings[filterMappings.size() - 1]
        def charEncodingFilterMapping = filterMappings.find {it.'filter-name'.text() == 'charEncodingFilter'}

        // add the Grails web request filter
        lastFilter + {
            filter {
                'filter-name'('grailsWebRequest')
                'filter-class'(org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequestFilter.getName())
            }
            if (grailsEnv == "development") {
                filter {
                    'filter-name'('reloadFilter')
                    'filter-class'(org.codehaus.groovy.grails.web.servlet.filter.GrailsReloadServletFilter.getName())
                }
            }
        }
        def grailsWebRequestFilter = {
            'filter-mapping' {
                'filter-name'('grailsWebRequest')
                'url-pattern'("/*")
            }
            if (grailsEnv == "development") {
                // Install the reload filter, which allows you to make
                // changes to artefacts and views while the app is
                // running.
                //
                // Note that no normal URIs map to the main Grails
                // servlet, so it's not possible to simply map the
                // filter to the servlet (the URL mappings filter
                // forwards the request to the servlet).
                //
                // The thing to note here is that we are assuming the
                // development environment is running in a container
                // that supports the 2.4 servlet spec.
                'filter-mapping' {
                    'filter-name'('reloadFilter')
                    'servlet-name'('grails')
                    'dispatcher'('FORWARD')
                }

                'filter-mapping' {
                     'filter-name'('reloadFilter')
                     'servlet-name'('gsp')
                 }
            }
        }
        if (charEncodingFilterMapping) {
            charEncodingFilterMapping + grailsWebRequestFilter
        }
        else {
            lastFilterMapping + grailsWebRequestFilter
        }

        // if we're in development environment first add a the reload filter
        // to the web.xml by finding the last filter and appending it after
        if (grailsEnv == "development") {
            // now find the GSP servlet and allow viewing generated source in
            // development mode
            def gspServlet = webXml.servlet.find {it.'servlet-name'?.text() == 'gsp'}
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



    def doWithDynamicMethods = {ApplicationContext ctx ->

        // add common objects and out variable for tag libraries
        def registry = GroovySystem.getMetaClassRegistry()

        def constructor = new DataBindingDynamicConstructor(ctx)
        def Closure constructorMapArg = {domainClass, Map params ->
            constructor.invoke(domainClass.clazz, [params] as Object[])
        }
        def consructorNoArgs = {domainClass ->
            constructor.invoke(domainClass.clazz, [] as Object[])
        }
        for (domainClass in application.domainClasses) {
            def mc = domainClass.metaClass
            mc.constructor = consructorNoArgs.curry(domainClass)
            mc.constructor = constructorMapArg.curry(domainClass)

            def setProps = new SetPropertiesDynamicProperty()
            mc.setProperties = {Object o ->
                setProps.set(delegate, o)
            }
            mc.getProperties = {->
                new LazyMetaPropertyMap(delegate)
            }
        }

        def namespaces = [] as HashSet

        for (taglib in application.tagLibClasses) {
            MetaClass mc = taglib.metaClass
            String namespace = taglib.namespace
            namespaces << namespace

            WebMetaUtils.registerCommonWebProperties(mc, application)

            mc.throwTagError = {String message ->
                throw new org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException(message)
            }
            mc.getPluginContextPath = {->
                PluginMetaManager metaManager = ctx.pluginMetaManager
                String path = metaManager.getPluginPathForResource(delegate.class.name)
                path ? path : ""
            }

            mc.getPageScope = {->
                def request = RCH.currentRequestAttributes().currentRequest
                def binding = request[GrailsApplicationAttributes.PAGE_SCOPE]
                if (!binding) {
                    binding = new Binding()
                    request[GrailsApplicationAttributes.PAGE_SCOPE] = binding
                }
                binding
            }

            mc.getOut = {->
                RCH.currentRequestAttributes().out
            }
            mc.setOut = {Writer newOut ->
                RCH.currentRequestAttributes().out = newOut
            }
            mc.getProperty = {String name ->
                MetaProperty metaProperty = mc.getMetaProperty(name)
                def result
                if (metaProperty) result = metaProperty.getProperty(delegate)
                else {
                    def ns = namespace ? namespace : GroovyPage.DEFAULT_NAMESPACE
                    def tagName = "${ns}:$name"

                    GrailsClass tagLibraryClass = application.getArtefactForFeature(
                            TagLibArtefactHandler.TYPE, tagName.toString())

                    if (tagLibraryClass) {
                        def tagLibrary = ctx.getBean(tagLibraryClass.fullName)
                        result = tagLibrary."$name".clone()
                        mc."${GCU.getGetterName(name)}" = {-> result}
                    }
                    else {

                        // try obtaining reference to tag lib via namespacing
                        tagLibraryClass = application.getArtefactForFeature(TagLibArtefactHandler.TYPE, name)

                        if (tagLibraryClass) {
                            result = new NamespacedTagDispatcher(tagLibraryClass.namespace, delegate.class, application, ctx)
                            mc."${GCU.getGetterName(name)}" = {-> result}
                        }
                        else {
                            throw new MissingPropertyException(name, delegate.class)
                        }

                    }
                }
                result
            }
            mc.invokeMethod = {String name, args ->
                args = args == null ? [] as Object[] : args
                def metaMethod
                synchronized (mc) {
                    metaMethod = mc.getMetaMethod(name, args)
                }
                def result
                if (metaMethod) result = metaMethod.invoke(delegate, args)
                else {
                    def ns = namespace ? namespace : GroovyPage.DEFAULT_NAMESPACE
                    def tagName = "${ns}:$name"
                    GrailsClass tagLibraryClass = application.getArtefactForFeature(

                            TagLibArtefactHandler.TYPE, tagName.toString())

                    if (!tagLibraryClass) {
                        tagName = "${GroovyPage.DEFAULT_NAMESPACE}:$name"
                        tagLibraryClass = application.getArtefactForFeature(TagLibArtefactHandler.TYPE, tagName.toString())
                    }

                    if (tagLibraryClass) {
                        synchronized (mc) {
                            WebMetaUtils.registerMethodMissingForTags(mc, ctx, tagLibraryClass, name)
                        }
                    }
                    if (mc.respondsTo(delegate, name, args)) {
                        result = mc.invokeMethod(delegate, name, args)
                    }
                    else {
                        // deal with the case where there is a closure property that could be invoked
                        MetaProperty metaProperty = mc.getMetaProperty(name)
                        def callable = metaProperty?.getProperty(delegate)
                        if (callable instanceof Closure) {
                            mc."$name" = {Object[] varArgs ->
                                varArgs ? callable.call(* varArgs) : callable.call()
                            }
                            result = args ? callable.call(* args) : callable.call()
                        }
                        else {
                            throw new MissingMethodException(name, delegate.class, args)
                        }
                    }
                }
                result
            }
            ctx.getBean(taglib.fullName).metaClass = mc
        }
        def bind = new BindDynamicMethod()
        // add commons objects and dynamic methods like render and redirect to controllers
        for (GrailsClass controller in application.controllerClasses) {
            MetaClass mc = controller.metaClass

            Class controllerClass = controller.clazz
            WebMetaUtils.registerCommonWebProperties(mc, application)
            registerControllerMethods(mc, ctx)
            registerMethodMissing(mc, application, ctx)
            Class superClass = controller.clazz.superclass

            for (ns in namespaces) {
                def propName = GCU.getGetterName(ns)
                if (!controller.hasProperty(ns)) {
                    def namespaceDispatcher = new NamespacedTagDispatcher(ns, controllerClass, application, ctx)
                    mc."$propName" = {-> namespaceDispatcher}
                }
            }

            mc.getPluginContextPath = {->
                PluginMetaManager metaManager = ctx.pluginMetaManager
                String path = metaManager.getPluginPathForResource(delegate.class.name)
                path ? path : ""
            }



            // deal with abstract super classes
            while (superClass != Object.class) {
                if (Modifier.isAbstract(superClass.getModifiers())) {
                    WebMetaUtils.registerCommonWebProperties(superClass.metaClass, application)
                    registerControllerMethods(superClass.metaClass, ctx)
                    registerMethodMissing(superClass.metaClass, application, ctx)
                }
                superClass = superClass.superclass
            }

            // look for actions that accept command objects and override
            // each of the actions to make command objects binding before executing
            for (actionName in controller.commandObjectActions) {
                def originalAction = controller.getPropertyValue(actionName)
                def paramTypes = originalAction.getParameterTypes()
                def closureName = actionName
                def commandObjectBindingAction = {Object[] varArgs ->

                    def commandObjects = []
                    for (v in varArgs) {
                        commandObjects << v
                    }
                    def counter = 0
                    for (paramType in paramTypes) {

                        if (GroovyObject.class.isAssignableFrom(paramType)) {
                            try {
                                def commandObject;
                                if (counter < commandObjects.size()) {
                                    if (paramType.isInstance(commandObjects[counter])) {
                                        commandObject = commandObjects[counter]
                                    }
                                }

                                if (!commandObject) {
                                    commandObject = paramType.newInstance()
                                    ctx.autowireCapableBeanFactory.autowireBeanProperties(commandObject,AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)
                                    commandObjects << commandObject
                                }
                                def params = RCH.currentRequestAttributes().params
                                bind.invoke(commandObject, "bindData", [commandObject, params] as Object[])
                                def errors = commandObject.errors ?: new BindException(commandObject, paramType.name)
                                def constrainedProperties = commandObject.constraints?.values()
                                constrainedProperties.each {constrainedProperty ->
                                    constrainedProperty.messageSource = ctx.getBean("messageSource")
                                    constrainedProperty.validate(commandObject, commandObject.getProperty(constrainedProperty.getPropertyName()), errors);
                                }
                                commandObject.errors = errors
                            } catch (Exception e) {
                                throw new ControllerExecutionException("Error occurred creating command object.", e);
                            }
                        }
                        counter++
                    }
                    GCU.getPropertyOrStaticPropertyOrFieldValue(delegate, closureName).call(* commandObjects)
                }
                mc."${GrailsClassUtils.getGetterName(actionName)}" = {->
                    commandObjectBindingAction.delegate = delegate
                    commandObjectBindingAction
                }
            }

            // look for actions that accept command objects and configure
            // each of the command object types
            def commandObjectClasses = controller.commandObjectClasses
            commandObjectClasses.each {commandObjectClass ->
                def commandObject = commandObjectClass.newInstance()
                def commandObjectMetaClass = commandObject.metaClass
                commandObjectMetaClass.setErrors = {Errors errors ->
                    RCH.currentRequestAttributes().setAttribute("${commandObjectClass.name}_errors", errors, 0)
                }
                commandObjectMetaClass.getErrors = {->
                    RCH.currentRequestAttributes().getAttribute("${commandObjectClass.name}_errors", 0)
                }

                commandObjectMetaClass.hasErrors = {->
                    errors?.hasErrors() ? true : false
                }
                def validationClosure = GCU.getStaticPropertyValue(commandObjectClass, 'constraints')
                if (validationClosure) {
                    def constrainedPropertyBuilder = new ConstrainedPropertyBuilder(commandObject)
                    validationClosure.setDelegate(constrainedPropertyBuilder)
                    validationClosure()
                    commandObjectMetaClass.constraints = constrainedPropertyBuilder.constrainedProperties
                } else {
                    commandObjectMetaClass.constraints = [:]
                }
            }
        }

    }

    def registerMethodMissing(MetaClass mc, GrailsApplication application, ApplicationContext ctx) {
        // allow controllers to call tag library methods
        mc.methodMissing = {String name, args ->
            args = args == null ? [] as Object[] : args

            def tagName = "${GroovyPage.DEFAULT_NAMESPACE}:$name"
            GrailsClass tagLibraryClass = application.getArtefactForFeature(
                    TagLibArtefactHandler.TYPE, tagName.toString())

            def result
            if (tagLibraryClass) {
                synchronized (mc) {
                    WebMetaUtils.registerMethodMissingForTags(mc, ctx, tagLibraryClass, name)
                }
                result = mc.invokeMethod(delegate, name, args)
            }
            else {
                throw new MissingMethodException(name, delegate.class, args)
            }
            result
        }

    }

    def registerControllerMethods(MetaClass mc, ApplicationContext ctx) {
        mc.getActionUri = {-> "/$controllerName/$actionName".toString()}
        mc.getControllerUri = {-> "/$controllerName".toString()}
        mc.getTemplateUri = {String name ->
            def webRequest = RCH.currentRequestAttributes()
            webRequest.attributes.getTemplateUri(name, webRequest.currentRequest)
        }
        mc.getViewUri = {String name ->
            def webRequest = RCH.currentRequestAttributes()
            webRequest.attributes.getViewUri(name, webRequest.currentRequest)
        }
        mc.setErrors = {Errors errors ->
            RCH.currentRequestAttributes().setAttribute(GrailsApplicationAttributes.ERRORS, errors, 0)
        }
        mc.getErrors = {->
            RCH.currentRequestAttributes().getAttribute(GrailsApplicationAttributes.ERRORS, 0)
        }
        mc.setModelAndView = {ModelAndView mav ->
            RCH.currentRequestAttributes().setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, mav, 0)
        }
        mc.getModelAndView = {->
            RCH.currentRequestAttributes().getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, 0)
        }
        mc.getChainModel = {->
            RCH.currentRequestAttributes().flashScope["chainModel"]
        }
        mc.hasErrors = {->
            errors?.hasErrors() ? true : false
        }

        def redirect = new RedirectDynamicMethod(ctx)
        def render = new RenderDynamicMethod()
        def bind = new BindDynamicMethod()
        // the redirect dynamic method
        mc.redirect = {Map args ->
            redirect.invoke(delegate, "redirect", args)
        }
        mc.chain = {Map args ->
            ChainMethod.invoke delegate, args
        }
        // the render method
        mc.render = {Object o ->
            render.invoke(delegate, "render", [o?.inspect()] as Object[])
        }

        mc.render = {String txt ->
            render.invoke(delegate, "render", [txt] as Object[])
        }
        mc.render = {Map args ->
            render.invoke(delegate, "render", [args] as Object[])
        }
        mc.render = {Closure c ->
            render.invoke(delegate, "render", [c] as Object[])
        }
        mc.render = {Map args, Closure c ->
            render.invoke(delegate, "render", [args, c] as Object[])
        }
        // the bindData method
        mc.bindData = {Object target, Object args ->
            bind.invoke(delegate, "bindData", [target, args] as Object[])
        }
        mc.bindData = {Object target, Object args, List disallowed ->
            bind.invoke(delegate, "bindData", [target, args, [exclude: disallowed]] as Object[])
        }
        mc.bindData = {Object target, Object args, List disallowed, String filter ->
            bind.invoke(delegate, "bindData", [target, args, [exclude: disallowed], filter] as Object[])
        }
        mc.bindData = {Object target, Object args, Map includeExclude ->
            bind.invoke(delegate, "bindData", [target, args, includeExclude] as Object[])
        }
        mc.bindData = {Object target, Object args, Map includeExclude, String filter ->
            bind.invoke(delegate, "bindData", [target, args, includeExclude, filter] as Object[])
        }
        mc.bindData = {Object target, Object args, String filter ->
            bind.invoke(delegate, "bindData", [target, args, filter] as Object[])
        }

    }


    def onChange = {event ->
        if (application.isArtefactOfType(ControllerArtefactHandler.TYPE, event.source)) {
            def context = event.ctx
            if (!context) {
                if (log.isDebugEnabled())
                    log.debug("Application context not found. Can't reload")
                return
            }
            boolean isNew = application.getControllerClass(event.source?.name) ? false : true
            def controllerClass = application.addArtefact(ControllerArtefactHandler.TYPE, event.source)



            if (isNew) {
                log.info "Controller ${event.source} added. Configuring.."
                // we can create the bean definitions from the oroginal configureAOPProxyBean closure
                // by currying it, which populates the values within the curried closure
                // once that is done we pass it to the "beans" method which will return a BeanBuilder
                def beanConfigs = configureAOPProxyBean.curry(controllerClass,
                        ControllerArtefactHandler.TYPE,
                        org.codehaus.groovy.grails.commons.GrailsControllerClass.class,
                        false)
                def beanDefinitions = beans(beanConfigs)
                // now that we have a BeanBuilder calling registerBeans and passing the app ctx will
                // register the necessary beans with the given app ctx
                beanDefinitions.registerBeans(event.ctx)

            }
            else {
                if (log.isDebugEnabled())
                    log.debug("Controller ${event.source} changed. Reloading...")

                def controllerTargetSource = context.getBean("${controllerClass.fullName}TargetSource")
                controllerTargetSource.swap(controllerClass)
            }

        }
        else if (application.isArtefactOfType(TagLibArtefactHandler.TYPE, event.source)) {
            boolean isNew = application.getTagLibClass(event.source?.name) ? false : true
            def taglibClass = application.addArtefact(TagLibArtefactHandler.TYPE, event.source)
            if (taglibClass) {
                // replace tag library bean
                def beanName = taglibClass.fullName
                def beans = beans {
                    "$beanName"(taglibClass.getClazz()) {bean ->
                        bean.autowire = true
                    }
                }
                beans.registerBeans(event.ctx)
            }
        }

        event.manager?.getGrailsPlugin("controllers")?.doWithDynamicMethods(event.ctx)
    }
}