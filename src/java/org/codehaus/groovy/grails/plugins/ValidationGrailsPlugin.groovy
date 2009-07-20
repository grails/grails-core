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

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU

import grails.util.GrailsUtil
import org.codehaus.groovy.grails.plugins.DomainClassPluginSupport
import org.codehaus.groovy.grails.support.SoftThreadLocalMap
import org.codehaus.groovy.grails.validation.ConstrainedPropertyBuilder
import org.codehaus.groovy.grails.validation.Validateable
import org.springframework.context.ApplicationContext
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors

public class ValidationGrailsPlugin {
    def version = GrailsUtil.getGrailsVersion()
    def dependsOn = [:]
    def loadAfter = ['hibernate', 'controllers']
    def typeFilters = [new AnnotationTypeFilter(Validateable)]

    static final PROPERTY_INSTANCE_MAP = new SoftThreadLocalMap()

    def doWithDynamicMethods = { ApplicationContext ctx ->
        // list of validateable classes
        def validateables = []

        // grab all of the classes specified in the application config
        application.config?.grails?.validateable?.classes?.each {
            validateables << it
        }

        for(entry in ctx.getBeansWithAnnotation(Validateable)) {
            Class validateable = entry?.value?.class
            if(validateable)
                validateables << validateable
        }

        // make all of these classes 'validateable'
        for(validateableClass in validateables) {
            log.debug "Making Class Validateable: ${validateableClass.name}"
            addValidationMethods(application, validateableClass, ctx)
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
                else {
                    return PROPERTY_INSTANCE_MAP.get().get(it)
                }
            }
            put = {key, val ->
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
            metaClass.validate = {->
                DomainClassPluginSupport.validateInstance(delegate, ctx)
            }
        }
    }
}
