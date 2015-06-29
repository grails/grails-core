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
package org.grails.plugins.web.filters
import grails.artefact.Controller
import grails.core.GrailsApplication
import grails.core.GrailsClass
import grails.util.GrailsClassUtils
import grails.web.api.WebAttributes
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.grails.plugins.web.filters.support.DelegateMetaMethod
import org.grails.plugins.web.filters.support.FilterConfigDelegateMetaMethodTargetStrategy
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.validation.Errors
import org.springframework.web.servlet.ModelAndView
/**
 * @author mike
 * @author Graeme Rocher
 */
class FilterConfig implements Controller {
    private static final long serialVersionUID = 4420245320722210200L;
    String name
    Map scope = [:]
    Closure before
    Closure after
    Closure afterView
    // this modelAndView overrides ControllersApi's modelAndView
    ModelAndView modelAndView
    boolean initialised = false

    FilterConfig() {
        initializeMetaClass()
    }

    void initializeMetaClass() {
        // use per-instance metaclass
        ExpandoMetaClass emc = new ExpandoMetaClass(getClass(), false, true)
        emc.initialize()
        setMetaClass(emc)
    }

    @Override
    GrailsApplication getGrailsApplication() {
        getGrailsAttributes().getGrailsApplication()
    }

    /**
     * This is the filters definition bean that declared the filter
     * config. Since it may contain injected services, etc., we
     * delegate any missing properties or methods to it.
     */
    def filtersDefinition

    /**
     * When the filter does not have a particular property, it passes
     * the request on to the filter definition class.
     */
    @CompileDynamic
    def propertyMissing(String propertyName) {
        // Delegate to the parent definition if it has this property.
        if (wiredFiltersDefinition.metaClass.hasProperty(wiredFiltersDefinition, propertyName)) {
            def getterName = GrailsClassUtils.getGetterName(propertyName)
            metaClass."$getterName" = { -> delegate.wiredFiltersDefinition.getProperty(propertyName) }
            return wiredFiltersDefinition."$propertyName"
        }

        throw new MissingPropertyException(propertyName, filtersDefinition.getClass())
    }

    def getWiredFiltersDefinition() {
        final webRequest = GrailsWebRequest.lookup()
        final GrailsClass grailsFilter = webRequest ? grailsApplication.getArtefact(FiltersConfigArtefactHandler.TYPE, filtersDefinition.class.name) : null
        if (grailsFilter) {
            applicationContext.getBean(grailsFilter.fullName)
        } else {
            return filtersDefinition
        }
    }

    /**
     * When the filter does not have a particular method, it passes
     * the call on to the filter definition class.
     */
    @CompileDynamic
    def methodMissing(String methodName, args) {
        // Delegate to the parent definition if it has this method.
        List<MetaMethod> respondsTo = filtersDefinition.metaClass.respondsTo(filtersDefinition, methodName, args)
        if (respondsTo) {
            // Use DelegateMetaMethod to proxy calls to actual MetaMethod for subsequent calls to this method
            DelegateMetaMethod dmm = new DelegateMetaMethod(respondsTo[0], FilterConfigDelegateMetaMethodTargetStrategy.instance)
            // register the metamethod to EMC
            metaClass.registerInstanceMethod(dmm)

            // for this invocation we still have to make the call
            return respondsTo[0].invoke(filtersDefinition, args)
        }

        // Ideally, we would throw a MissingMethodException here
        // whether the filter config is intialised or not. However,
        // if it's in the initialisation phase, the MME gets swallowed somewhere.
        if (!initialised) {
            throw new IllegalStateException(
                    "Invalid filter definition in ${wiredFiltersDefinition.getClass().name} - trying to call method '${methodName}' outside of an interceptor.")
        }

        // The required method was not found on the parent filter definition either.
        throw new MissingMethodException(methodName, wiredFiltersDefinition.getClass(), args)
    }

    String toString() { "FilterConfig[$name, scope=$scope]" }

    @Override
    Errors getErrors() { null }

}