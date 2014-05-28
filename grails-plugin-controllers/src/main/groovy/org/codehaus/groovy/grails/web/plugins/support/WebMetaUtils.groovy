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

import grails.databinding.DataBindingSource;
import grails.databinding.SimpleMapDataBindingSource;
import grails.validation.ValidationErrors

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.plugins.DomainClassPluginSupport
import org.codehaus.groovy.grails.validation.ConstraintsEvaluator
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator
import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods
import org.springframework.context.ApplicationContext
import org.springframework.validation.Errors
import org.springframework.web.context.request.RequestContextHolder as RCH

import java.lang.reflect.Method

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

    /**
     * Use getCommandObjectBindingSourceForPrefix instead.  
     * 
     * @see #getCommandObjectBindingSourceForPrefix(String, DataBindingSource)
     * @deprecated
     */
    @Deprecated
    static DataBindingSource getCommandObjectBindingSource(Class commandObjectClass, DataBindingSource params) {
        def commandParamsKey = convertTypeNameToParamsPrefix(commandObjectClass)
        getCommandObjectBindingSourceForPrefix commandParamsKey, params
    }

    /**
     * Return a DataBindingSource for a command object which has a parameter name matching the specified prefix.
     * If params include something like widget.name=Thing and prefix is widget then the returned binding source
     * will include name=thing, not widget.name=Thing.
     * 
     * @param prefix The parameter name for the command object
     * @param params The original binding source associated with the request
     * @return The binding source suitable for binding to a command object with a parameter name matching the specified prefix.
     */
    static DataBindingSource getCommandObjectBindingSourceForPrefix(String prefix, DataBindingSource params) {
        def commandParams = params
        if (params != null && prefix != null) {
            def innerValue = params[prefix]
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
    static void registerCommonWebProperties(MetaClass mc, GrailsApplication application) {
        ControllerDynamicMethods.registerCommonWebProperties(mc, application)
    }
}
