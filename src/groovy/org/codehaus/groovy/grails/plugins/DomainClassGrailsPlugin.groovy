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

import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.validation.GrailsDomainClassValidator
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.beans.BeanUtils

/**
* A plug-in that configures the domain classes in the spring context
*
* @author Graeme Rocher
* @since 0.4
*/
class DomainClassGrailsPlugin {
	
	def version = grails.util.GrailsUtil.getGrailsVersion()
	def dependsOn = [i18n:version]
	def loadAfter = ['controllers']
	
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
                grailsApplication = ref("grailsApplication", true)
            }

		}
	}
          
	static final PROPERTY_INSTANCE_MAP = new org.codehaus.groovy.grails.support.SoftThreadLocalMap()	
	
	def doWithDynamicMethods = { ApplicationContext ctx->
           enhanceDomainClasses(application, ctx)
	}


    static enhanceDomainClasses(GrailsApplication application, ApplicationContext ctx) {
        for(GrailsDomainClass dc in application.domainClasses) {
			def domainClass = dc
            MetaClass metaClass = domainClass.metaClass

            metaClass.ident = {-> delegate[domainClass.identifier.name] }
            metaClass.constructor = {->
                if(ctx.containsBean(domainClass.fullName)) {
                    ctx.getBean(domainClass.fullName)
                }
                else {
                    BeanUtils.instantiateClass(domainClass.clazz)
                }
            }
            metaClass.static.create = {-> ctx.getBean(domainClass.getFullName()) }

            addValidationMethods(application, domainClass, ctx)
            addRelationshipManagementMethods(domainClass)
        }
    }
    

    private static addValidationMethods(GrailsApplication application, GrailsDomainClass dc, ApplicationContext ctx) {
        def metaClass = dc.metaClass
        def domainClass = dc

        metaClass.'static'.getConstraints = {->
            domainClass.constrainedProperties
        }

        metaClass.getConstraints = {->
            domainClass.constrainedProperties
        }

        metaClass.hasErrors = {-> delegate.errors?.hasErrors() }

        def get
        def put
        try {
            def rch = application.classLoader.loadClass("org.springframework.web.context.request.RequestContextHolder")
            get = {
                def attributes = rch.getRequestAttributes()
                if(attributes) {
                    return attributes.request.getAttribute(it)
                }
                else {
                    return PROPERTY_INSTANCE_MAP.get().get(it)
                }
            }
            put = { key, val ->
                def attributes = rch.getRequestAttributes()
                if(attributes) {
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

        metaClass.getErrors = {->
            def errors
            def key = "org.codehaus.groovy.grails.ERRORS_${delegate.class.name}_${System.identityHashCode(delegate)}"
            errors = get(key)
            if(!errors) {
                errors =  new BeanPropertyBindingResult( delegate, delegate.getClass().getName())
                put key, errors
            }
            errors
           }
        metaClass.setErrors = { Errors errors ->
            def key = "org.codehaus.groovy.grails.ERRORS_${delegate.class.name}_${System.identityHashCode(delegate)}"
            put key, errors
        }
        metaClass.clearErrors = {->
            delegate.setErrors (new BeanPropertyBindingResult(delegate, delegate.getClass().getName()))
        }
        if (!metaClass.respondsTo(dc.getReference(), "validate")) {
            metaClass.validate = {->
                DomainClassPluginSupport.validateInstance(delegate, ctx)
            }
        }
    }


    private static addRelationshipManagementMethods(GrailsDomainClass dc) {
        def metaClass = dc.metaClass
        for(p in dc.persistantProperties) {
            def prop = p
            if(prop.basicCollectionType) {
                def collectionName = GrailsClassUtils.getClassNameRepresentation(prop.name)
                metaClass."addTo$collectionName" = { obj ->
                    if(prop.referencedPropertyType.isInstance(obj)) {
                        if (delegate[prop.name] == null) {
                            delegate[prop.name] = GrailsClassUtils.createConcreteCollection(prop.type)
                        }
                        delegate[prop.name] << obj
                        return delegate
                    }
                    else {
                        throw new MissingMethodException("addTo${collectionName}", dc.clazz, [arg] as Object[])
                    }
                }
                metaClass."removeFrom$collectionName" = { obj ->
                    if(delegate[prop.name]) {
                        delegate[prop.name].remove(obj)
                    }
                    return delegate
                }
            }
            else if(prop.oneToOne || prop.manyToOne) {
                def identifierPropertyName = "${prop.name}Id"
                if(!metaClass.hasProperty(dc.reference.wrappedInstance,identifierPropertyName)) {
                    def getterName = GrailsClassUtils.getGetterName(identifierPropertyName)
                    metaClass."$getterName" = {-> GrailsDomainConfigurationUtil.getAssociationIdentifier(delegate, prop.name, prop.referencedDomainClass) }
                }
            }
            else if (prop.oneToMany || prop.manyToMany) {
                if (metaClass instanceof ExpandoMetaClass) {
                    def propertyName = prop.name
                    def collectionName = GrailsClassUtils.getClassNameRepresentation(propertyName)
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
                        if (prop.bidirectional && prop.otherSide) {
                            def otherSide = prop.otherSide
                            if(otherSide.oneToMany || otherSide.manyToMany) {
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