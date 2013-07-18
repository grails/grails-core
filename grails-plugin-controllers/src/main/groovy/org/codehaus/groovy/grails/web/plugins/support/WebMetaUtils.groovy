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
package org.codehaus.groovy.grails.web.plugins.support

import grails.artefact.Enhanced
import grails.validation.ValidationErrors
import groovy.transform.CompileStatic

import java.lang.reflect.Method

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsTagLibClass
import org.codehaus.groovy.grails.plugins.DomainClassPluginSupport
import org.codehaus.groovy.grails.validation.ConstraintsEvaluator
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator
import org.codehaus.groovy.grails.web.pages.GroovyPage
import org.codehaus.groovy.grails.web.pages.TagLibraryLookup
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.web.util.StreamCharBuffer
import org.grails.databinding.DataBindingSource
import org.grails.databinding.SimpleMapDataBindingSource
import org.springframework.context.ApplicationContext
import org.springframework.validation.Errors
import org.springframework.web.context.request.RequestContextHolder as RCH

/**
 * Provides utility methods used to support meta-programming. In particular commons methods to
 * register tag library method invokations as new methods an a given MetaClass.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class WebMetaUtils {

    /**
     * Prepares a command object binding action for usage
     *
     * @param action The binding action
     * @param originalAction The original action to be replacec
     * @param actionName The action name
     * @param controller The controller
     * @return The new binding action
     */
    static Closure prepareCommandObjectBindingAction(Closure action, Closure originalAction,
                                                     String actionName, Object controller, ApplicationContext ctx) {
        def commandObjectAction = action.curry(originalAction, actionName)
        controller.getClass().metaClass."${GrailsClassUtils.getGetterName(actionName)}" = {->
            def actionDelegate = commandObjectAction.clone()
            actionDelegate.delegate = delegate
            actionDelegate
        }
        for (type in originalAction.parameterTypes) {
            enhanceCommandObject ctx, type
        }
        commandObjectAction.delegate = controller
        return commandObjectAction
    }

    /**
     * Prepares a command object binding action for usage
     *
     * @param action The binding action
     */
    @SuppressWarnings("rawtypes")
    static void prepareCommandObjectBindingAction(Method action, Class[] commandObjectClasses, ApplicationContext ctx) {
        for (type in commandObjectClasses) {
            enhanceCommandObject ctx, type
        }
    }

    static DataBindingSource getCommandObjectBindingSource(Class commandObjectClass, DataBindingSource params) {
        def commandParamsKey = convertTypeNameToParamsPrefix(commandObjectClass)
        def commandParams = params
        if (params != null && commandParamsKey != null) {
            def innerValue = params[commandParamsKey]
            if(innerValue instanceof DataBindingSource) {
                commandParams = innerValue
            } else if(innerValue instanceof Map) {
                commandParams = new SimpleMapDataBindingSource(innerValue)
            }
        }
        commandParams
    }

    private static String convertTypeNameToParamsPrefix(Class clazz) {
        def result = clazz?.simpleName?.replaceAll(/(\B[A-Z])/, '-$1')?.toLowerCase()
        if (result?.endsWith("-command")) {
            return result.substring(0, result.size() - 8)
        }
        return null
    }

    /**
     * Checks whether the given action is a command object action
     * @param callable The action to check
     * @return true if it is a command object action
     */
    static boolean isCommandObjectAction(Closure callable) {
        def paramTypes = callable.parameterTypes
        paramTypes && paramTypes[0] != Object[].class && paramTypes[0] != Object
    }

    /**
     * Enhances a command object with new capabilities such as validation and constraints handling
     *
     * @param commandObjectClass The command object class
     */
    static void enhanceCommandObject(ApplicationContext ctx, Class commandObjectClass) {

        def commandObjectMetaClass = commandObjectClass.metaClass
        if (!commandObjectMetaClass.respondsTo("grailsEnhanced")) {

            commandObjectMetaClass.setErrors = { Errors errors ->
                RCH.currentRequestAttributes().setAttribute(
                        "${commandObjectClass.name}_${System.identityHashCode(delegate)}_errors", errors, 0)
            }

            commandObjectMetaClass.getErrors = {->
                def errors = RCH.currentRequestAttributes().getAttribute(
                        "${commandObjectClass.name}_${System.identityHashCode(delegate)}_errors", 0)
                if (!errors) {
                    errors = new ValidationErrors(delegate)
                    RCH.currentRequestAttributes().setAttribute(
                            "${commandObjectClass.name}_${System.identityHashCode(delegate)}_errors", errors, 0)
                }
                return errors
            }

            commandObjectMetaClass.hasErrors = {-> errors?.hasErrors() ? true : false }
            commandObjectMetaClass.validate = {->
                DomainClassPluginSupport.validateInstance(delegate, ctx)
            }
            def constraintsEvaluator
            if (ctx?.containsBean(ConstraintsEvaluator.BEAN_NAME)) {
                constraintsEvaluator = ctx.getBean(ConstraintsEvaluator.BEAN_NAME)
            }
            else {
                constraintsEvaluator = new DefaultConstraintEvaluator()
            }
            def constrainedProperties = constraintsEvaluator.evaluate(commandObjectClass)
            commandObjectMetaClass.getConstraints = {-> constrainedProperties }

            commandObjectMetaClass.clearErrors = {->
                delegate.setErrors(new ValidationErrors(delegate))
            }
            commandObjectMetaClass.grailsEnhanced = {-> true }
        }
    }

    /**
     * This creates the difference dynamic methods and properties on the controllers. Most methods
     * are implemented by looking up the current request from the RequestContextHolder (RCH)
     */
    static registerCommonWebProperties(MetaClass mc, GrailsApplication application) {
        def paramsObject = {-> RCH.currentRequestAttributes().params }
        def flashObject = {-> RCH.currentRequestAttributes().flashScope }
        def sessionObject = {-> RCH.currentRequestAttributes().session }
        def requestObject = {-> RCH.currentRequestAttributes().currentRequest }
        def responseObject = {-> RCH.currentRequestAttributes().currentResponse }
        def servletContextObject = {-> RCH.currentRequestAttributes().servletContext }
        def grailsAttrsObject = {-> RCH.currentRequestAttributes().attributes }

        // the params object
        mc.getParams = paramsObject
        // the flash object
        mc.getFlash = flashObject
        // the session object
        mc.getSession = sessionObject
        // the request object
        mc.getRequest = requestObject
        // the servlet context
        mc.getServletContext = servletContextObject
        // the response object
        mc.getResponse = responseObject
        // The GrailsApplicationAttributes object
        mc.getGrailsAttributes = grailsAttrsObject
        // The GrailsApplication object
        mc.getGrailsApplication = {-> RCH.currentRequestAttributes().attributes.grailsApplication }

        mc.getActionName = {-> RCH.currentRequestAttributes().actionName }
        mc.getControllerName = {-> RCH.currentRequestAttributes().controllerName }
        mc.getWebRequest = {-> RCH.currentRequestAttributes() }
    }

    // used for testing (GroovyPageUnitTestMixin.mockTagLib) and "nonEnhancedTagLibClasses" in GroovyPagesGrailsPlugin
    static void enhanceTagLibMetaClass(final GrailsTagLibClass taglib, TagLibraryLookup gspTagLibraryLookup) {
        if (!taglib.clazz.getAnnotation(Enhanced)) {
            final MetaClass mc = taglib.getMetaClass()
            final String namespace = taglib.namespace ?: GroovyPage.DEFAULT_NAMESPACE

            for (tag in taglib.tagNames) {
                WebMetaUtils.registerMethodMissingForTags(mc, gspTagLibraryLookup, namespace, tag)
            }
            // propertyMissing and methodMissing are now added in MetaClassEnhancer / TagLibraryApi
        }
    }

    @CompileStatic
    static registerMethodMissingForTags(MetaClass metaClass, TagLibraryLookup gspTagLibraryLookup, String namespace, String name) {
        GroovyObject mc = (GroovyObject)metaClass;
        mc.setProperty(name) {Map attrs, Closure body ->
            GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, attrs, body, GrailsWebRequest.lookup())
        }
        mc.setProperty(name) {Map attrs, CharSequence body ->
            GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, attrs, new GroovyPage.ConstantClosure(body), GrailsWebRequest.lookup())
        }
        mc.setProperty(name) {Map attrs ->
            GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, attrs, null, GrailsWebRequest.lookup())
        }
        mc.setProperty(name) {Closure body ->
            GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, [:], body, GrailsWebRequest.lookup())
        }
        mc.setProperty(name) {->
            GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, [:], null, GrailsWebRequest.lookup())
        }
    }

    static registerMethodMissingForTags(MetaClass mc, ApplicationContext ctx,
                                        GrailsTagLibClass tagLibraryClass, String name) {
        //def tagLibrary = ctx.getBean(tagLibraryClass.fullName)
        TagLibraryLookup gspTagLibraryLookup = ctx.getBean("gspTagLibraryLookup")
        String namespace = tagLibraryClass.namespace ?: GroovyPage.DEFAULT_NAMESPACE
        registerMethodMissingForTags(mc, gspTagLibraryLookup, namespace, name)
    }

    static registerStreamCharBufferMetaClass() {
        StreamCharBuffer.metaClass.methodMissing = { String name, args ->
            def retval = delegate.toString().invokeMethod(name, args)
            StreamCharBuffer.metaClass."$name" = { Object[] varArgs ->
                delegate.toString().invokeMethod(name, varArgs)
            }
            retval
        }

        StreamCharBuffer.metaClass.asType = { Class clazz ->
            if (clazz == String) {
                delegate.toString()
            } else if (clazz == char[]) {
                delegate.toCharArray()
            } else if (clazz == Boolean || clazz == boolean) {
                delegate.asBoolean()
            } else {
                delegate.toString().asType(clazz)
            }
        }
    }

    static void registerPropertyMissingForTag(MetaClass mc, String name, Object result) {
        mc."${GrailsClassUtils.getGetterName(name)}" = {-> result }
    }
}
