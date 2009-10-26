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
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsControllerClass
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.plugins.DefaultGrailsPlugin
import org.codehaus.groovy.grails.scaffolding.DefaultGrailsTemplateGenerator
import org.codehaus.groovy.grails.scaffolding.GrailsTemplateGenerator
import org.codehaus.groovy.grails.scaffolding.view.ScaffoldingViewResolver
import org.springframework.beans.PropertyValue
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.ApplicationContext;

/**
* A plug-in that handles the configuration of dynamic scaffolding in Grails
*
* @author Graeme Rocher
* @since 0.4
*/
class ScaffoldingGrailsPlugin {

    static final LOG  = LogFactory.getLog(DefaultGrailsPlugin)
	def version = grails.util.GrailsUtil.getGrailsVersion()
	def dependsOn = [controllers:version, groovyPages:version]
	def observe = ['controllers']
    def loadAfter = ['controllers']
	
    def doWithSpring = {
        ScaffoldingViewResolver.clearViewCache()
        
        scaffoldedActionMap(HashMap)
        controllerToScaffoldedDomainClassMap(HashMap)
        scaffoldingTemplateGenerator(DefaultGrailsTemplateGenerator, ref("classLoader"))
        BeanDefinition beanDef = getBeanDefinition("jspViewResolver")

        jspViewResolver(ScaffoldingViewResolver) {
            templateGenerator = scaffoldingTemplateGenerator
            scaffoldedActionMap = ref("scaffoldedActionMap")
            scaffoldedDomains = controllerToScaffoldedDomainClassMap
            // copy values from other bean def
            if(beanDef) {
                for(PropertyValue pv in beanDef.getPropertyValues().getPropertyValueList()) {
                    delegate."${pv.name}" = pv.value
                }
            }
        }
    }

    def doWithApplicationContext = { ApplicationContext appCtx ->
        for(GrailsControllerClass controllerClass in application.controllerClasses) {
            configureScaffoldingController(appCtx, application, controllerClass)
        }
    }

    private configureScaffoldingController(ApplicationContext appCtx, application, GrailsControllerClass controllerClass) {
        GrailsTemplateGenerator generator = appCtx.getBean("scaffoldingTemplateGenerator")
        ClassLoader parentLoader = appCtx.getBean("classLoader")
        Map scaffoldedActionMap = appCtx.getBean("scaffoldedActionMap")
        Map scaffoldedDomains = appCtx.getBean("controllerToScaffoldedDomainClassMap")

        def javaClass = controllerClass.clazz
        def metaClass = javaClass.metaClass

        def scaffoldProperty = GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(controllerClass.getReference().getWrappedInstance(), "scaffold")
        if (scaffoldProperty) {
            GrailsDomainClass domainClass = getScaffoldedDomainClass(application, controllerClass, scaffoldProperty)
            scaffoldedActionMap[controllerClass.logicalPropertyName] = []
            if (domainClass) {
                scaffoldedDomains[controllerClass.logicalPropertyName] = domainClass
                String controllerSource = generateControllerSource(generator, domainClass)
                def scaffoldedInstance = createScaffoldedInstance(parentLoader, controllerSource)
                List actionProperties = getScaffoldedActions(scaffoldedInstance)

                for (MetaProperty actionProp in actionProperties) {
                    if (actionProp) {
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
            }
            else {
                LOG.error "Cannot generate controller logic for scaffolded class ${scaffoldProperty}. It is not a domain class!"
            }
        }
    }

    def onChange = { event ->
        ScaffoldingViewResolver.clearViewCache()
        if(event.source && application.isControllerClass(event.source)) {
            GrailsControllerClass controllerClass = application.getControllerClass(event.source.name)            
            configureScaffoldingController(event.ctx, event.application, controllerClass)
        }

    }

    private GrailsDomainClass getScaffoldedDomainClass(application, GrailsControllerClass controllerClass, scaffoldProperty) {
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

    private createScaffoldedInstance(ClassLoader parentLoader, String controllerSource) {
        def classLoader = new GroovyClassLoader(parentLoader)
        def scaffoldedControllerClass = classLoader.parseClass(controllerSource)
        def scaffoldedInstance = scaffoldedControllerClass.newInstance()
        return scaffoldedInstance
    }

    private List getScaffoldedActions(scaffoldedInstance) {
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

    private String generateControllerSource(GrailsTemplateGenerator generator, GrailsDomainClass domainClass) {
        def sw = new StringWriter()
        LOG.info "Generating controller logic for scaffolding domain: ${domainClass.fullName}"
        generator.generateController(domainClass, sw)
        String controllerSource = sw.toString()
        return controllerSource
    }



}
