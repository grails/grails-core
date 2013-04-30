/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.plugins.converters

import grails.artefact.Enhanced
import grails.converters.JSON
import grails.converters.XML
import groovy.transform.CompileStatic

import javax.servlet.http.HttpServletRequest

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsMetaClassUtils
import org.codehaus.groovy.grails.commons.metaclass.MetaClassEnhancer
import org.codehaus.groovy.grails.plugins.converters.api.ConvertersApi
import org.grails.datastore.mapping.validation.ValidationErrors
import org.springframework.context.ApplicationContext
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors

/**
 * @author Graeme Rocher
 * @since 2.0
 */
class ConvertersPluginSupport {

    /**
     * Enhances a Grails application with the Converters capability
     *
     * @param application
     * @param applicationContext
     */
    @CompileStatic
    static void enhanceApplication(GrailsApplication application, ApplicationContext applicationContext) {
        MetaClassEnhancer enhancer = new MetaClassEnhancer()
        enhancer.addApi(new ConvertersApi(applicationContext:applicationContext))

        // Override GDK asType for some common Interfaces and Classes
        enhancer.enhanceAll([Errors,BeanPropertyBindingResult, ValidationErrors, ValidationErrors, ArrayList, TreeSet, HashSet, List, Set, Collection, GroovyObject, Object, Enum].collect {  Class c ->
            GrailsMetaClassUtils.getExpandoMetaClass(c)
        })

        enhanceRequest()
        enhanceDomainClasses(application, enhancer)
    }

    static void enhanceDomainClasses(GrailsApplication grailsApplication, MetaClassEnhancer metaClassEnhancer) {
        for(GrailsDomainClass dc in grailsApplication.domainClasses) {
            if (!dc.getClazz().getAnnotation(Enhanced)) {
                metaClassEnhancer.enhance(dc.metaClass)
            }
        }
    }

    private static void enhanceRequest() {
        // Methods for Reading JSON/XML from Requests
        def getXMLMethod = { -> XML.parse((HttpServletRequest) delegate) }
        def getJSONMethod = { -> JSON.parse((HttpServletRequest) delegate) }

        def requestMc = GroovySystem.metaClassRegistry.getMetaClass(HttpServletRequest)
        requestMc.getXML = getXMLMethod
        requestMc.getJSON = getJSONMethod
    }
}
