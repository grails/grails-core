/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.webflow.engine.builder

import org.springframework.web.context.request.RequestContextHolder as RCH

import grails.util.GrailsNameUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.GrailsDomainConfigurationUtil
import org.codehaus.groovy.grails.web.binding.GrailsDataBinder
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.validation.Errors
import org.springframework.webflow.action.AbstractAction
import org.springframework.webflow.core.collection.LocalAttributeMap
import org.springframework.webflow.execution.Event
import org.springframework.webflow.execution.RequestContext

 /**
 * Invokes a closure as a Webflow action placing the returned model within the flow scope.
 *
 * @author Graeme Rocher
 * @since 0.6
 */
class ClosureInvokingAction extends AbstractAction {

    private static final Log LOG = LogFactory.getLog(ClosureInvokingAction)
    private static final String RESULT = "result"

    Closure callable
    def commandClasses
    def noOfParams
    boolean hasCommandObjects
    def applicationContext

    ClosureInvokingAction(Closure callable) {
        this.callable = callable
        this.commandClasses = callable.parameterTypes
        this.noOfParams = commandClasses.size()
        this.hasCommandObjects = noOfParams > 1 || (noOfParams == 1 && commandClasses[0] != Object.class && commandClasses[0] != RequestContext.class)
        if (hasCommandObjects) {
            for (co in commandClasses) {
                co.metaClass.getErrors = {->
                    RCH.currentRequestAttributes().getAttribute("${co.name}_${delegate.hashCode()}_errors",0)
                }
                co.metaClass.setErrors = { Errors errors ->
                    RCH.currentRequestAttributes().setAttribute("${co.name}_${delegate.hashCode()}_errors",errors,0)
                }
                co.metaClass.hasErrors = {-> errors?.hasErrors() ? true : false }
                def constrainedProperties = GrailsDomainConfigurationUtil.evaluateConstraints(co.newInstance())
                co.metaClass.getConstraints = {-> constrainedProperties }
                co.metaClass.validate = {->
                    errors = new org.springframework.validation.BeanPropertyBindingResult(delegate, delegate.class.name)
                    def localErrors = errors

                    checkAppContext()
                    if (constrainedProperties) {
                        for (prop in constrainedProperties.values()) {
                            prop.messageSource = applicationContext.getBean("messageSource")
                            prop.validate(delegate, delegate.getProperty(prop.getPropertyName()), localErrors)
                        }
                    }
                    !localErrors.hasErrors()
                }
            }
        }
    }

    def checkAppContext() {
        if (!applicationContext) {
            def webRequest = RCH.currentRequestAttributes()
            applicationContext = webRequest.attributes.applicationContext
        }
    }

    protected Event doExecute(RequestContext context) throws Exception {

        def result
        try {
            Closure cloned = callable.clone()
            def actionDelegate = new ActionDelegate(this, context)
            cloned.delegate = actionDelegate
            cloned.resolveStrategy = Closure.DELEGATE_FIRST

            if (hasCommandObjects) {
                checkAppContext()
                def commandInstances = []
                for (p in commandClasses) {
                    def instance = p.newInstance()

                    applicationContext.autowireCapableBeanFactory?.autowireBeanProperties(
                        instance, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)

                    def params = noOfParams > 1 ? actionDelegate.params[GrailsNameUtils.getPropertyName(instance.class)] : actionDelegate.params
                    if (params) {
                        def binder = GrailsDataBinder.createBinder(instance, instance.class.name, actionDelegate.request)
                        binder.bind(params)
                    }
                    instance.validate()
                    commandInstances << instance
                }
                result = cloned.call(*commandInstances)
            }
            else {
                result = cloned.call(context)
            }
            def event
            if (result instanceof Map) {
                context.flowScope.putAll(new LocalAttributeMap(result))
                event = super.success(result)
            }
            else if (result instanceof Event) {
                event = result
                context.flowScope.putAll(event.attributes)
            }
            else {
                event = super.success(result)
            }
            // here we place any errors that have occured in groovy objects into flashScope so they
            // can be restored upon deserialization of the flow
            checkForErrors(context,context.flowScope.asMap())
            checkForErrors(context,context.conversationScope.asMap())
            return event
        }
        catch (Throwable e) {
            LOG.error("Exception occured invoking flow action: ${e.message}", e)
            throw e
        }
    }

    def checkForErrors(context, scope) {
        for (entry in scope) {
            try {
                if (entry.value instanceof GroovyObject) {
                    def errors = entry.value.errors
                    if (errors?.hasErrors()) {
                        context.flashScope.put("${GrailsApplicationAttributes.ERRORS}_${entry.key}", errors)
                    }
                }
            }
            catch (MissingPropertyException e) {
                // ignore
            }
        }
    }

    Object invokeMethod(String name, args) {
        if (metaClass instanceof ExpandoMetaClass) {
            def emc = metaClass
            MetaMethod metaMethod = emc.getMetaMethod(name, args)
            if (metaMethod) return metaMethod.invoke(this, args)
            return invokeMethodAsEvent(name, args)
        }
        return invokeMethodAsEvent(name, args)
    }

    private Object invokeMethodAsEvent(String name, args) {
        if (args.length == 0) {
            return result(name)
        }

        if (args[0] instanceof Map) {
            return result(name,new LocalAttributeMap(args[0]))
        }

        return result(name,name, args)
    }
}
