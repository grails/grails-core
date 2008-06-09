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

import org.codehaus.groovy.grails.validation.*
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.springframework.beans.BeanUtils
import org.springframework.validation.Errors
import org.springframework.web.context.request.RequestContextHolder as RCH
import org.springframework.validation.BeanPropertyBindingResult
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsDomainConfigurationUtil
import org.springframework.context.ApplicationContext

/**
* A plug-in that configures the domain classes in the spring context
*
* @author Graeme Rocher
* @since 0.4
*/
class DomainClassGrailsPlugin {
	
	def version = grails.util.GrailsUtil.getGrailsVersion()
	def dependsOn = [i18n:version]
	def loadAfter = ['hibernate', 'controllers']
	
	def doWithSpring = {
		for(dc in application.domainClasses) {
		    // Note the use of Groovy's ability to use dynamic strings in method names!
		    "${dc.fullName}"(dc.getClazz()) { bean ->
				bean.singleton = false
				bean.autowire = "byName"						
			}
			"${dc.fullName}DomainClass"(MethodInvokingFactoryBean) {
				targetObject = ref("grailsApplication", true)
				targetMethod = "getArtefact"
				arguments = [DomainClassArtefactHandler.TYPE, dc.fullName]
			}
			"${dc.fullName}PersistentClass"(MethodInvokingFactoryBean) {
				targetObject = ref("${dc.fullName}DomainClass")
				targetMethod = "getClazz"        						
            }
            "${dc.fullName}Validator"(GrailsDomainClassValidator) {
                messageSource = ref("messageSource")
                domainClass = ref("${dc.fullName}DomainClass")                
            }

		}
	}
          
	static final PROPERTY_INSTANCE_MAP = new org.codehaus.groovy.grails.support.SoftThreadLocalMap()	
	
	def doWithDynamicMethods = { ctx->
        for(GrailsDomainClass dc in application.domainClasses) {
			def domainClass = dc
            MetaClass metaClass = domainClass.metaClass

            metaClass.ident = {-> delegate[domainClass.identifier.name] }

            metaClass.'static'.create = {-> BeanUtils.instantiateClass(domainClass.getClazz()) }

            addValidationMethods(dc, ctx)
            addRelationshipManagementMethods(dc)
        }
	}

    private addValidationMethods(GrailsDomainClass dc, ApplicationContext ctx) {
        def metaClass = dc.metaClass
        def domainClass = dc

        metaClass.'static'.getConstraints = {->
            domainClass.constrainedProperties
        }

        metaClass.getConstraints = {->
            domainClass.constrainedProperties
        }

        metaClass.hasErrors = {-> delegate.errors?.hasErrors() }
        metaClass.getErrors = {->
            def request = RCH.getRequestAttributes()?.request
            def errors
            def storage = request ? request : PROPERTY_INSTANCE_MAP.get()
            def key = "org.codehaus.groovy.grails.ERRORS_${delegate.class.name}_${System.identityHashCode(delegate)}"
            errors = storage[key]
            if(!errors) {
                errors =  new BeanPropertyBindingResult( delegate, delegate.getClass().getName())
                storage[key] = errors
            }
            errors
           }
        metaClass.setErrors = { Errors errors ->
            def request = RCH.getRequestAttributes()?.request
            def storage = request ? request : PROPERTY_INSTANCE_MAP.get()
            def key = "org.codehaus.groovy.grails.ERRORS_${delegate.class.name}_${System.identityHashCode(delegate)}"
            storage[key] = errors
        }
        metaClass.clearErrors = {->
            delegate.setErrors (new BeanPropertyBindingResult(delegate, delegate.getClass().getName()))
        }
        if (!metaClass.respondsTo(dc.getReference(), "validate")) {
            metaClass.validate = {->
                def localErrors = new org.springframework.validation.BeanPropertyBindingResult(delegate, delegate.class.name)
                for (prop in constraints.values()) {
                    prop.messageSource = ctx.getBean("messageSource")
                    prop.validate(delegate, delegate.getProperty(prop.getPropertyName()), localErrors);
                }

                if (localErrors.hasErrors()) {
                    delegate.errors.addAllErrors(localErrors)
                }

                !delegate.errors.hasErrors()
            }
        }
    }

    private addRelationshipManagementMethods(GrailsDomainClass dc) {
        def metaClass = dc.metaClass
        for(p in dc.persistantProperties) {
            def prop = p
            if(prop.oneToOne || prop.manyToOne) {
                def identifierPropertyName = "${prop.name}Id"
                if(!metaClass.hasProperty(dc.reference.wrappedInstance,identifierPropertyName)) {
                    def getterName = GrailsClassUtils.getGetterName(identifierPropertyName)
                    metaClass."$getterName" = {-> GrailsDomainConfigurationUtil.getAssociationIdentifier(delegate, prop.name, prop.referencedDomainClass) }
                }
            }
            else if (prop.oneToMany || prop.manyToMany) {
                if (metaClass instanceof ExpandoMetaClass) {
                    def propertyName = prop.name
                    def collectionName = propertyName.size() == 1 ? propertyName.toUpperCase() : "${propertyName[0].toUpperCase()}${propertyName[1..-1]}"
                    def otherDomainClass = prop.referencedDomainClass

                    metaClass."addTo${collectionName}" = {Object arg ->
                        Object obj
                        if (delegate[prop.name] == null) {
                            delegate[prop.name] = GrailsClassUtils.createConcreteCollection(prop.type)
                        }
                        if (arg instanceof Map) {
                            obj = otherDomainClass.newInstance()
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
                        if (prop.bidirectional) {
                            if (prop.manyToMany) {
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