package org.codehaus.groovy.grails.plugins

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU

import grails.util.GrailsUtil
import org.codehaus.groovy.grails.support.SoftThreadLocalMap
import org.codehaus.groovy.grails.validation.ConstrainedPropertyBuilder
import org.codehaus.groovy.grails.validation.Validateable
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors

public class ValidationGrailsPlugin {
    def version = GrailsUtil.getGrailsVersion()
    def dependsOn = [:]
    def loadAfter = ['hibernate', 'controllers']
    static final PROPERTY_INSTANCE_MAP = new SoftThreadLocalMap()

    def doWithDynamicMethods = {ctx ->
        // list of validateable classes
        def validateables = []

        // grab all of the classes specified in the application config
        application.config?.grails?.validateable?.classes?.each {
            validateables << it
        }

        // grab all of the classes annotated with @Validateable
        def simpleRegistry = new SimpleBeanDefinitionRegistry();
        try {
            def scanner = new ClassPathBeanDefinitionScanner(simpleRegistry, false)
            scanner.setIncludeAnnotationConfig(false)
            scanner.addIncludeFilter(new AnnotationTypeFilter(Validateable))
            def packagesToScan = application.config?.grails?.validateable?.packages
            if(!packagesToScan) {
                packagesToScan = ['']
            }
            scanner.scan(packagesToScan as String[])
        }
        catch (e) {
            // Workaround for http://jira.springframework.org/browse/SPR-5120
            log.warn "WARNING: Cannot scan for @Validateable due to container classloader issues. This feature has been disabled for this environment. Message: ${e.message}"
        };

        simpleRegistry.beanDefinitionNames?.each {beanDefinitionName ->
            def beanDefinition = simpleRegistry.getBeanDefinition(beanDefinitionName)
            def beanClass = application.classLoader.loadClass(beanDefinition.beanClassName)
            validateables << beanClass
        }

        // make all of these classes 'validateable'
        validateables.each {validateableClass ->
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
