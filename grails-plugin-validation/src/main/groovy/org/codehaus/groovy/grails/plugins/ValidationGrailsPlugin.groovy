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

import grails.util.GrailsUtil

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.codehaus.groovy.grails.lifecycle.ShutdownOperations
import org.codehaus.groovy.grails.support.SoftThreadLocalMap
import org.codehaus.groovy.grails.validation.ConstrainedPropertyBuilder
import org.codehaus.groovy.grails.web.plugins.support.ValidationSupport
import org.springframework.context.ApplicationContext
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors

class ValidationGrailsPlugin {

    def version = GrailsUtil.getGrailsVersion()
    def loadAfter = ['hibernate', 'hibernate4', 'controllers']

    static final ThreadLocal PROPERTY_INSTANCE_MAP = new SoftThreadLocalMap()

    static {
        ShutdownOperations.addOperation({
            PROPERTY_INSTANCE_MAP.remove()
        } as Runnable)
    }

    def doWithDynamicMethods = { ApplicationContext ctx ->
        // grab all of the classes specified in the application config
        application.config?.grails?.validateable?.classes?.each { validateableClass ->
            if (validateableClass instanceof Class) {
                log.debug "Making Class Validateable: ${validateableClass.name}"
                addValidationMethods(application, validateableClass, ctx)
            }
        }
    }

    private static addValidationMethods(application, validateableClass, ctx) {
        def metaClass = validateableClass.metaClass
        metaClass.hasErrors = {-> delegate.errors?.hasErrors() }

        def get
        def put
        try {
            def rch = application.classLoader.loadClass("org.springframework.web.context.request.RequestContextHolder")
            get = {
                def attributes = rch.getRequestAttributes()
                if (attributes) {
                    return attributes.request.getAttribute(it)
                }
                return PROPERTY_INSTANCE_MAP.get().get(it)
            }
            put = { key, val ->
                def attributes = rch.getRequestAttributes()
                if (attributes) {
                    attributes.request.setAttribute(key, val)
                }
                else {
                    PROPERTY_INSTANCE_MAP.get().put(key, val)
                }
            }
        }
        catch (Throwable e) {
            get = { PROPERTY_INSTANCE_MAP.get().get(it) }
            put = {key, val -> PROPERTY_INSTANCE_MAP.get().put(key, val) }
        }

        metaClass.getErrors = {->
            def errors
            def key = "org.codehaus.groovy.grails.ERRORS_${delegate.class.name}_${System.identityHashCode(delegate)}"
            errors = get(key)
            if (!errors) {
                errors = new BeanPropertyBindingResult(delegate, delegate.getClass().getName())
                put key, errors
            }
            errors
        }
        metaClass.setErrors = {Errors errors ->
            def key = "org.codehaus.groovy.grails.ERRORS_${delegate.class.name}_${System.identityHashCode(delegate)}"
            put key, errors
        }
        metaClass.clearErrors = {->
            delegate.setErrors(new BeanPropertyBindingResult(delegate, delegate.getClass().getName()))
        }

        def validationClosure = GCU.getStaticPropertyValue(validateableClass, 'constraints')
        def validateable = validateableClass.newInstance()
        if (validationClosure) {
            def constrainedPropertyBuilder = new ConstrainedPropertyBuilder(validateable)
            validationClosure.setDelegate(constrainedPropertyBuilder)
            validationClosure()
            metaClass.constraints = constrainedPropertyBuilder.constrainedProperties
        }
        else {
            metaClass.constraints = [:]
        }

        if (!metaClass.respondsTo(validateable, "validate")) {
            metaClass.validate = { ->
                ValidationSupport.validateInstance delegate
            }
        }
    }
}
