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
package org.codehaus.groovy.grails.plugins.scaffolding

import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsControllerClass
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.plugins.DefaultGrailsPlugin
import org.codehaus.groovy.grails.scaffolding.DefaultGrailsTemplateGenerator
import org.codehaus.groovy.grails.scaffolding.GrailsTemplateGenerator
import org.codehaus.groovy.grails.scaffolding.view.ScaffoldingViewResolver
import org.springframework.beans.PropertyValue
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.ApplicationContext

 /**
 * Handles the configuration of dynamic scaffolding in Grails.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
class ScaffoldingGrailsPlugin {

    static final LOG = LogFactory.getLog(DefaultGrailsPlugin)

    def version = grails.util.GrailsUtil.getGrailsVersion()
    def dependsOn = [controllers:version, groovyPages:version]
    def observe = ['controllers', 'domainClass']
    def loadAfter = ['controllers']

    def doWithSpring = {
        ScaffoldingViewResolver.clearViewCache()

        scaffoldedActionMap(HashMap)
        controllerToScaffoldedDomainClassMap(HashMap)
        scaffoldingTemplateGenerator(DefaultGrailsTemplateGenerator, ref("classLoader")) {
            grailsApplication = ref("grailsApplication")
        }
        BeanDefinition beanDef = getBeanDefinition("jspViewResolver")

        jspViewResolver(ScaffoldingViewResolver) {
            templateGenerator = scaffoldingTemplateGenerator
            scaffoldedActionMap = ref("scaffoldedActionMap")
            scaffoldedDomains = controllerToScaffoldedDomainClassMap
            // copy values from other bean def
            if (beanDef) {
                for (PropertyValue pv in beanDef.getPropertyValues().getPropertyValueList()) {
                    delegate."${pv.name}" = pv.value
                }
            }
        }
    }

    def doWithApplicationContext = { ApplicationContext ctx ->
        if (!application.warDeployed) {
            Thread.start {
                configureScaffolding(ctx, application)
            }
        }
        else {
            configureScaffolding(ctx, application)
        }
    }

    def configureScaffolding(ApplicationContext appCtx, app) {
        for (controllerClass in app.controllerClasses) {
            configureScaffoldingController(appCtx, app, controllerClass)
        }
    }

    public static configureScaffoldingController(ApplicationContext appCtx, GrailsApplication application, GrailsControllerClass controllerClass) {

        def scaffoldProperty = controllerClass.getPropertyValue("scaffold", Object)
        if (!scaffoldProperty || !appCtx) {
            return
        }

        Map scaffoldedActionMap = appCtx.getBean("scaffoldedActionMap")
        GrailsDomainClass domainClass = getScaffoldedDomainClass(application, controllerClass, scaffoldProperty)
        scaffoldedActionMap[controllerClass.logicalPropertyName] = []
        if (!domainClass) {
            LOG.error "Cannot generate controller logic for scaffolded class ${scaffoldProperty}. It is not a domain class!"
            return
        }

        GrailsTemplateGenerator generator = appCtx.getBean("scaffoldingTemplateGenerator")
        ClassLoader parentLoader = appCtx.getBean("classLoader")

        Map scaffoldedDomains = appCtx.getBean("controllerToScaffoldedDomainClassMap")
        scaffoldedDomains[controllerClass.logicalPropertyName] = domainClass
        String controllerSource = generateControllerSource(generator, domainClass)
        def scaffoldedInstance = createScaffoldedInstance(parentLoader, controllerSource)
        List actionProperties = getScaffoldedActions(scaffoldedInstance)

        def javaClass = controllerClass.clazz
        def metaClass = javaClass.metaClass

        for (MetaProperty actionProp in actionProperties) {
            if (!actionProp) {
                continue
            }

            String propertyName = actionProp.name
            def mp = metaClass.getMetaProperty(propertyName)
            scaffoldedActionMap[controllerClass.logicalPropertyName] << propertyName

            if (!mp) {
                Closure propertyValue = actionProp.getProperty(scaffoldedInstance)
                metaClass."${GrailsClassUtils.getGetterName(propertyName)}" = {->
                    propertyValue.delegate = delegate
                    propertyValue.resolveStrategy = Closure.DELEGATE_FIRST
                    propertyValue
                }
            }
            controllerClass.registerMapping(propertyName)
            if (propertyName == GrailsControllerClass.INDEX_ACTION) {
                controllerClass.defaultActionName = propertyName
            }
        }
    }

    def onChange = { event ->
        ScaffoldingViewResolver.clearViewCache()
        if (event.source && application.isControllerClass(event.source)) {
            GrailsControllerClass controllerClass = application.getControllerClass(event.source.name)
            configureScaffoldingController(event.ctx, event.application, controllerClass)
        }
        else {
            configureScaffolding(event.ctx, event.application)
        }
    }

    private static GrailsDomainClass getScaffoldedDomainClass(application, GrailsControllerClass controllerClass, scaffoldProperty) {
        GrailsDomainClass domainClass = null

        if (scaffoldProperty) {
            if (scaffoldProperty instanceof Class) {
                domainClass = application.getDomainClass(scaffoldProperty.name)
            }
            else if (scaffoldProperty) {
                scaffoldProperty = controllerClass.packageName ? "${controllerClass.packageName}.${controllerClass.name}" : controllerClass.name
                domainClass = application.getDomainClass(scaffoldProperty)
            }
        }
        return domainClass
    }

    private static createScaffoldedInstance(ClassLoader parentLoader, String controllerSource) {
        def classLoader = new GroovyClassLoader(parentLoader)
        def scaffoldedControllerClass = classLoader.parseClass(controllerSource)
        return scaffoldedControllerClass.newInstance()
    }

    private static List getScaffoldedActions(scaffoldedInstance) {
        def actionProperties = scaffoldedInstance.metaClass.properties.collect {MetaProperty mp ->
            try {
                def val = mp.getProperty(scaffoldedInstance)
                if (val instanceof Closure) return mp
            }
            catch (Exception e) {
                // ignore
            }
        }
        return actionProperties
    }

    private static String generateControllerSource(GrailsTemplateGenerator generator, GrailsDomainClass domainClass) {
        def sw = new StringWriter()
        LOG.info "Generating controller logic for scaffolding domain: ${domainClass.fullName}"
        generator.generateController(domainClass, sw)
        return sw.toString()
    }
}
