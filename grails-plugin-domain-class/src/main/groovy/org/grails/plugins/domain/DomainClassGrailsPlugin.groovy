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
package org.grails.plugins.domain

import grails.config.Config
import grails.core.ComponentCapableDomainClass
import grails.core.GrailsApplication
import grails.core.GrailsDomainClass
import grails.plugins.Plugin
import grails.util.GrailsUtil
import grails.validation.ConstraintsEvaluator
import groovy.transform.CompileStatic
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.core.legacy.LegacyGrailsApplication
import org.grails.datastore.gorm.config.GrailsDomainClassMappingContext
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.grails.plugins.domain.support.GrailsDomainClassCleaner
import org.grails.validation.ConstraintEvalUtils
import org.grails.validation.ConstraintsEvaluatorFactoryBean
import org.grails.validation.GrailsDomainClassValidator
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.context.ApplicationContext
/**
 * Configures the domain classes in the spring context.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
class DomainClassGrailsPlugin extends Plugin {

    def watchedResources = ["file:./grails-app/domain/**/*.groovy",
                            "file:./plugins/*/grails-app/domain/**/*.groovy"]

    def version = GrailsUtil.getGrailsVersion()
    def dependsOn = [i18n:version]
    def loadAfter = ['controllers', 'dataSource']

    GrailsApplication grailsApplication

    Closure doWithSpring() {{->

        def application = grailsApplication
        def config = application.config
        def defaultConstraintsMap = getDefaultConstraints(config)

        "${ConstraintsEvaluator.BEAN_NAME}"(ConstraintsEvaluatorFactoryBean) {
             defaultConstraints = defaultConstraintsMap
        }

        grailsDomainClassMappingContext(GrailsDomainClassMappingContext, application)
        
        grailsDomainClassCleaner(GrailsDomainClassCleaner, application)

        for (dc in application.domainClasses) {
            // Note the use of Groovy's ability to use dynamic strings in method names!
            if (dc.abstract) {
                continue
            }

            "${dc.fullName}"(dc.clazz) { bean ->
                bean.singleton = false
                bean.autowire = "byName"
            }
            "${dc.fullName}DomainClass"(MethodInvokingFactoryBean) { bean ->
                targetObject = application
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
                delegate.grailsApplication = application
            }
        }
    }}

    static getDefaultConstraints(Config config) {
        ConstraintEvalUtils.getDefaultConstraints(config)
    }

    @Override
    void doWithDynamicMethods() {
        enhanceDomainClasses(grailsApplication, applicationContext)
    }

    @Override
    void onChange(Map<String, Object> event) {
        def cls = event.source
        try {
            ClassPropertyFetcher.@cachedClassPropertyFetchers.clear()
        } catch (e) {
            // restricted environment, ignore
        }

        if (!(cls instanceof Class)) {
            return
        }


        def application = grailsApplication
        final domainClass = application.addArtefact(DomainClassArtefactHandler.TYPE, cls)
        if (domainClass.abstract) {
            return
        }

        beans {
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
                application = ref("grailsApplication", true)
            }
        }
        enhanceDomainClasses(application, applicationContext)
        application.refreshConstraints()
    }

    void onConfigChange(Map<String, Object> event) {
        ConstraintEvalUtils.clearDefaultConstraints()
        def beans = beans {
            def defaultConstraintsMap = getDefaultConstraints(event.source)
            "${ConstraintsEvaluator.BEAN_NAME}"(ConstraintsEvaluatorFactoryBean) {
                 defaultConstraints = defaultConstraintsMap
            }
        }
        beans.registerBeans(applicationContext)
        grailsApplication.refreshConstraints()
    }


    @CompileStatic
    static void enhanceDomainClasses(org.codehaus.groovy.grails.commons.GrailsApplication application, ApplicationContext ctx) {
        enhanceDomainClasses(((LegacyGrailsApplication)application).grailsApplication, ctx)
    }

    static void  enhanceDomainClasses(GrailsApplication application, ApplicationContext ctx) {
        for (GrailsDomainClass dc in application.domainClasses) {
            def domainClass = dc
            if (dc instanceof ComponentCapableDomainClass) {
                for (GrailsDomainClass component in dc.getComponents()) {
                    if (!application.isDomainClass(component.clazz)) {
                        registerConstraintsProperty(component.metaClass, component)
                    }
                }
            }
            MetaClass metaClass = domainClass.metaClass
            registerConstraintsProperty(metaClass, domainClass)
            metaClass.getDomainClass = {-> domainClass }
            AutowireCapableBeanFactory autowireCapableBeanFactory = ctx.autowireCapableBeanFactory
            int byName = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME
            metaClass.static.autowireDomain = { instance ->
                autowireCapableBeanFactory.autowireBeanProperties(instance, byName, false)
            }
        }
    }


    /**
     * Registers the constraints property for the given MetaClass and domainClass instance
     */
    static void registerConstraintsProperty(MetaClass metaClass, GrailsDomainClass domainClass) {
        metaClass.static.getConstraints = { -> domainClass.constrainedProperties }

        metaClass.getConstraints = {-> domainClass.constrainedProperties }
    }

}
