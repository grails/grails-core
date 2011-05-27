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

import org.springframework.web.context.request.RequestContextHolder as RCH

import java.lang.reflect.Method
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsTagLibClass
import org.codehaus.groovy.grails.plugins.DomainClassPluginSupport
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator
import org.codehaus.groovy.grails.web.metaclass.BindDynamicMethod
import org.codehaus.groovy.grails.web.pages.GroovyPage
import org.codehaus.groovy.grails.web.pages.TagLibraryLookup
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.ControllerExecutionException
import org.codehaus.groovy.grails.web.util.StreamCharBuffer
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.codehaus.groovy.grails.validation.ConstraintsEvaluator

/**
 * Provides utility methods used to support meta-programming. In particular commons methods to
 * register tag library method invokations as new methods an a given MetaClass.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class WebMetaUtils {

    static Closure createAndPrepareCommandObjectAction(GroovyObject controller, Closure originalAction,
                                                       String actionName, ApplicationContext ctx) {
        def bindingAction = createCommandObjectBindingAction(ctx)
        prepareCommandObjectBindingAction bindingAction, originalAction, actionName, controller, ctx
    }

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

    /**
     * Creates a command object binding action that can be used to replace an existing action
     *
     * @param ctx The ApplicationContext
     * @return The command object binding action
     */
    static Closure createCommandObjectBindingAction(ApplicationContext ctx) {
        def bind = new BindDynamicMethod()
        return {Closure originalAction, String closureName, Object[] varArgs ->

            def paramTypes = originalAction.getParameterTypes()
            def commandObjects = []
            for (v in varArgs) {
                commandObjects << v
            }
            def counter = 0
            def params = RCH.currentRequestAttributes().params
            for (paramType in paramTypes) {
                if (GroovyObject.isAssignableFrom(paramType)) {
                    try {
                        def commandObject
                        if (counter < commandObjects.size()) {
                            if (paramType.isInstance(commandObjects[counter])) {
                                commandObject = commandObjects[counter]
                            }
                        }

                        if (!commandObject) {
                            commandObject = paramType.newInstance()
                            ctx.autowireCapableBeanFactory?.autowireBeanProperties(
                                    commandObject, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)
                            commandObjects << commandObject
                        }

                        def commandParamsKey = convertTypeNameToParamsPrefix(paramType)
                        def commandParams = params
                        if (params != null && commandParamsKey != null && params[commandParamsKey] instanceof Map) {
                            commandParams = params[commandParamsKey]
                        }

                        bind.invoke(commandObject, "bindData", [commandObject, commandParams] as Object[])
                        def errors = commandObject.errors ?: new BindException(commandObject, paramType.name)
                        def constrainedProperties = commandObject.constraints?.values()
                        for (constrainedProperty in constrainedProperties) {
                            constrainedProperty.messageSource = ctx.getBean("messageSource")
                            constrainedProperty.validate(commandObject, commandObject.getProperty(
                                    constrainedProperty.getPropertyName()), errors)
                        }
                        commandObject.errors = errors
                    }
                    catch (Exception e) {
                        throw new ControllerExecutionException("Error occurred creating command object.", e)
                    }
                }
                counter++
            }
            def callable = GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(delegate, closureName)
            callable.call(* commandObjects)
        }
    }

    private static String convertTypeNameToParamsPrefix(Class clazz) {
        def result = clazz?.simpleName?.replaceAll(/(\B[A-Z])/, '-$1')?.toLowerCase()
        if (result?.endsWith("-command")) {
            return result.substring(0, result.size() - 8)
        } else {
            return null
        }
    }

    /**
     * Checks whether the given action is a command object action
     * @param callable The action to check
     * @return True if it is a command object action
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
                    errors = new BeanPropertyBindingResult(delegate, delegate.getClass().getName())
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
                delegate.setErrors(new BeanPropertyBindingResult(delegate, delegate.getClass().getName()))
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

    static registerMethodMissingForTags(MetaClass mc, TagLibraryLookup gspTagLibraryLookup, String namespace, String name) {
        mc."$name" = {Map attrs, Closure body ->
            GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, attrs, body, RCH.currentRequestAttributes())
        }
        mc."$name" = {Map attrs, CharSequence body ->
            delegate."$name"(attrs, new GroovyPage.ConstantClosure(body))
        }
        mc."$name" = {Map attrs ->
            GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, attrs, null, RCH.currentRequestAttributes())
        }
        mc."$name" = {Closure body ->
            GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, [:], body, RCH.currentRequestAttributes())
        }
        mc."$name" = {->
            GroovyPage.captureTagOutput(gspTagLibraryLookup, namespace, name, [:], null, RCH.currentRequestAttributes())
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
            }
            else if (clazz == char[]) {
                delegate.toCharArray()
            }
            else {
                delegate.toString().asType(clazz)
            }
        }
    }

    public static void registerPropertyMissingForTag(MetaClass mc, String name, Object result) {
        mc."${GrailsClassUtils.getGetterName(name)}" = {-> result }
    }
}
