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

import grails.artefact.Enhanced
import grails.util.ClosureToMapPopulator
import grails.util.GrailsUtil

import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.domain.GormApiSupport
import org.codehaus.groovy.grails.domain.GrailsDomainClassMappingContext
import org.codehaus.groovy.grails.support.SoftThreadLocalMap
import org.codehaus.groovy.grails.validation.ConstraintsEvaluator
import org.codehaus.groovy.grails.validation.ConstraintsEvaluatorFactoryBean
import org.codehaus.groovy.grails.validation.GrailsDomainClassValidator
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.datastore.mapping.model.MappingContext
import org.springframework.datastore.mapping.simple.SimpleMapDatastore
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.validation.Validator

/**
 * A plugin that configures the domain classes in the spring context.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
class DomainClassGrailsPlugin {

    def watchedResources = ["file:./grails-app/domain/**/*.groovy",
                            "file:./plugins/*/grails-app/domain/**/*.groovy"]

    def version = GrailsUtil.getGrailsVersion()
    def dependsOn = [i18n:version]
    def loadAfter = ['controllers']

    def doWithSpring = {

        def config = application.config
        def defaultConstraintsMap = getDefaultConstraints(config)

        "${ConstraintsEvaluator.BEAN_NAME}"(ConstraintsEvaluatorFactoryBean) {
             defaultConstraints = defaultConstraintsMap
        }

        grailsDomainClassMappingContext(GrailsDomainClassMappingContext, application)

        for (dc in application.domainClasses) {
            // Note the use of Groovy's ability to use dynamic strings in method names!
            if (!dc.abstract) {
                "${dc.fullName}"(dc.clazz) { bean ->
                    bean.singleton = false
                    bean.autowire = "byName"
                }
                "${dc.fullName}DomainClass"(MethodInvokingFactoryBean) { bean ->
                    targetObject = ref("grailsApplication", true)
                    targetMethod = "getArtefact"
                    bean.lazyInit = true
                    arguments = [DomainClassArtefactHandler.TYPE, dc.fullName]
                }
                "${dc.fullName}PersistentClass"(MethodInvokingFactoryBean) { bean ->
                    targetObject = ref("${dc.fullName}DomainClass")
                    bean.lazyInit = true
                    targetMethod = "getClazz"
                }
                "${dc.fullName}Validator"(GrailsDomainClassValidator) { bean ->
                    messageSource = ref("messageSource")
                    bean.lazyInit = true
                    domainClass = ref("${dc.fullName}DomainClass")
                    grailsApplication = ref("grailsApplication", true)
                }
            }

        }
    }

    public static getDefaultConstraints(ConfigObject config) {
        def constraints = config?.grails?.gorm?.default?.constraints
        def defaultConstraintsMap = null
        if (constraints instanceof Closure) {
            defaultConstraintsMap = new ClosureToMapPopulator().populate((Closure<?>) constraints);
        }
        return defaultConstraintsMap
    }

    static final PROPERTY_INSTANCE_MAP = new SoftThreadLocalMap()

    def doWithDynamicMethods = { ApplicationContext ctx->
        enhanceDomainClasses(application, ctx)
    }

    def onChange = { event ->
        def cls = event.source

        if (cls instanceof Class) {
            final domainClass = application.addArtefact(DomainClassArtefactHandler.TYPE, cls)
            if (!domainClass.abstract) {
                def beans = beans {
                    "${domainClass.fullName}"(domainClass.clazz) { bean ->
                        bean.singleton = false
                        bean.autowire = "byName"
                    }
                    "${domainClass.fullName}DomainClass"(MethodInvokingFactoryBean) { bean ->
                        targetObject = ref("grailsApplication", true)
                        targetMethod = "getArtefact"
                        bean.lazyInit = true
                        arguments = [DomainClassArtefactHandler.TYPE, domainClass.fullName]
                    }
                    "${domainClass.fullName}PersistentClass"(MethodInvokingFactoryBean) { bean ->
                        targetObject = ref("${domainClass.fullName}DomainClass")
                        bean.lazyInit = true
                        targetMethod = "getClazz"
                    }
                    "${domainClass.fullName}Validator"(GrailsDomainClassValidator) { bean ->
                        messageSource = ref("messageSource")
                        bean.lazyInit = true
                        domainClass = ref("${domainClass.fullName}DomainClass")
                        grailsApplication = ref("grailsApplication", true)
                    }
                }
                beans.registerBeans(event.ctx)
                enhanceDomainClasses(event.application, event.ctx)
            }
        }
    }

    def onConfigChange = { event ->
        def beans = beans {
            def defaultConstraintsMap = getDefaultConstraints(event.source)
            "${ConstraintsEvaluator.BEAN_NAME}"(ConstraintsEvaluatorFactoryBean) {
                 defaultConstraints = defaultConstraintsMap
            }
        }
        beans.registerBeans(event.ctx)
        event.application.refreshConstraints()
    }

    static enhanceDomainClasses(GrailsApplication application, ApplicationContext ctx) {

        final mappingContext = ctx.getBean("grailsDomainClassMappingContext", MappingContext)
        def datastore = new SimpleMapDatastore(mappingContext, ctx)

        for (GrailsDomainClass dc in application.domainClasses) {
            def domainClass = dc
            def isEnhanced = dc.clazz.getAnnotation(Enhanced) != null
            if (dc instanceof ComponentCapableDomainClass) {
                for (GrailsDomainClass component in dc.getComponents()) {
                    if (!application.isDomainClass(component.clazz)) {
                        registerConstraintsProperty(component.metaClass, component)
                    }
                }
            }
            MetaClass metaClass = domainClass.metaClass

            registerConstraintsProperty(metaClass, domainClass)
            addRelationshipManagementMethods(domainClass, ctx)

            metaClass.getDomainClass = {-> domainClass }
            if (!isEnhanced) {
                if (!dc.abstract) {
                    metaClass.constructor = { ->
                        getDomainInstance domainClass, ctx
                    }
                }
                metaClass.ident = {-> delegate[domainClass.identifier.name] }
                metaClass.static.create = { ->
                    getDomainInstance domainClass, ctx
                }
                addValidationMethods(application, domainClass, ctx)
            }
            else {
                if (!domainClass.abstract) {
                    Validator validator = ctx.getBean("${domainClass.fullName}Validator", Validator)
                    def gormValidationApi = null
                    metaClass.static.currentGormValidationApi = {->
                        // lazy initialize this, since in all likelihood this method will be overriden by the hibernate plugin
                        if (gormValidationApi == null) {
                            gormValidationApi = GormApiSupport.getGormValidationApi(datastore, domainClass.clazz, validator)
                        }
                        return gormValidationApi
                    }
                }
                metaClass.static.autowireDomain = { instance ->
                    ctx.autowireCapableBeanFactory.autowireBeanProperties(instance,AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)
                }
            }
        }
    }

    private static addValidationMethods(GrailsApplication application, GrailsDomainClass dc, ApplicationContext ctx) {
        def metaClass = dc.metaClass
        def domainClass = dc

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
                    attributes.request.setAttribute(key,val)
                }
                else {
                    PROPERTY_INSTANCE_MAP.get().put(key,val)
                }
            }
        }
        catch (Throwable e) {
            get = { PROPERTY_INSTANCE_MAP.get().get(it) }
            put = { key, val -> PROPERTY_INSTANCE_MAP.get().put(key,val) }
        }

        metaClass.getErrors = { ->
            def errors
            def key = "org.codehaus.groovy.grails.ERRORS_${delegate.class.name}_${System.identityHashCode(delegate)}"
            errors = get(key)
            if (!errors) {
                errors =  new BeanPropertyBindingResult(delegate, delegate.getClass().getName())
                put key, errors
            }
            errors
        }
        metaClass.setErrors = { Errors errors ->
            def key = "org.codehaus.groovy.grails.ERRORS_${delegate.class.name}_${System.identityHashCode(delegate)}"
            put key, errors
        }
        metaClass.clearErrors = { ->
            delegate.setErrors (new BeanPropertyBindingResult(delegate, delegate.getClass().getName()))
        }
        if (!domainClass.hasMetaMethod("validate")) {
            metaClass.validate = { ->
                DomainClassPluginSupport.validateInstance(delegate, ctx)
            }
        }
    }

    /**
     * Registers the constraints property for the given MetaClass and domainClass instance
     */
    static void registerConstraintsProperty(MetaClass metaClass, GrailsDomainClass domainClass) {
        metaClass.'static'.getConstraints = { -> domainClass.constrainedProperties }

        metaClass.getConstraints = {-> domainClass.constrainedProperties }
    }

    private static getDomainInstance(domainClass, ctx) {
        def obj
        if (ctx.containsBean(domainClass.fullName)) {
            obj = ctx.getBean(domainClass.fullName)
        }
        else {
            obj = BeanUtils.instantiateClass(domainClass.clazz)
        }
        obj
    }
    private static addRelationshipManagementMethods(GrailsDomainClass dc, ApplicationContext ctx) {
        def metaClass = dc.metaClass
        for (p in dc.persistantProperties) {
            def prop = p
            if (prop.basicCollectionType) {
                def collectionName = GrailsClassUtils.getClassNameRepresentation(prop.name)
                metaClass."addTo$collectionName" = { obj ->
                    if (obj instanceof CharSequence && !(obj instanceof String)) {
                        obj = obj.toString()
                    }
                    if (prop.referencedPropertyType.isInstance(obj)) {
                        if (delegate[prop.name] == null) {
                            delegate[prop.name] = GrailsClassUtils.createConcreteCollection(prop.type)
                        }
                        delegate[prop.name] << obj
                        return delegate
                    }
                    else {
                        throw new MissingMethodException("addTo${collectionName}", dc.clazz, [obj] as Object[])
                    }
                }
                metaClass."removeFrom$collectionName" = { obj ->
                    if (delegate[prop.name]) {
                        if (obj instanceof CharSequence && !(obj instanceof String)) {
                            obj = obj.toString()
                        }
                        delegate[prop.name].remove(obj)
                    }
                    return delegate
                }
            }
            else if (prop.oneToOne || prop.manyToOne) {
                def identifierPropertyName = "${prop.name}Id"
                if (!dc.hasMetaProperty(identifierPropertyName)) {
                    def getterName = GrailsClassUtils.getGetterName(identifierPropertyName)
                    metaClass."$getterName" = {-> GrailsDomainConfigurationUtil.getAssociationIdentifier(
                        delegate, prop.name, prop.referencedDomainClass) }
                }
            }
            else if (prop.oneToMany || prop.manyToMany) {
                if (metaClass instanceof ExpandoMetaClass) {
                    def propertyName = prop.name
                    def collectionName = GrailsClassUtils.getClassNameRepresentation(propertyName)
                    def otherDomainClass = prop.referencedDomainClass

                    metaClass."addTo${collectionName}" = { Object arg ->
                        Object obj
                        if (delegate[prop.name] == null) {
                            delegate[prop.name] = GrailsClassUtils.createConcreteCollection(prop.type)
                        }
                        if (arg instanceof Map) {
                            obj = getDomainInstance(otherDomainClass, ctx)
                            obj.properties = arg
                            delegate[prop.name].add(obj)
                        }
                        else if (otherDomainClass.clazz.isInstance(arg)) {
                            obj = arg
                            delegate[prop.name].add(obj)
                        }
                        else {
                            throw new MissingMethodException("addTo${collectionName}", dc.clazz, [arg] as Object[])
                        }
                        if (prop.bidirectional && prop.otherSide) {
                            def otherSide = prop.otherSide
                            if (otherSide.oneToMany || otherSide.manyToMany) {
                                String name = prop.otherSide.name
                                if (!obj[name]) {
                                    obj[name] = GrailsClassUtils.createConcreteCollection(prop.otherSide.type)
                                }
                                obj[prop.otherSide.name].add(delegate)
                            }
                            else {
                                obj[prop.otherSide.name] = delegate
                            }
                        }
                        delegate
                    }
                    metaClass."removeFrom${collectionName}" = {Object arg ->
                        if (otherDomainClass.clazz.isInstance(arg)) {
                            delegate[prop.name]?.remove(arg)
                            if (prop.bidirectional) {
                                if (prop.manyToMany) {
                                    String name = prop.otherSide.name
                                    arg[name]?.remove(delegate)
                                }
                                else {
                                    arg[prop.otherSide.name] = null
                                }
                            }
                        }
                        else {
                            throw new MissingMethodException("removeFrom${collectionName}", dc.clazz, [arg] as Object[])
                        }
                        delegate
                    }
                }
            }
        }
    }
}
