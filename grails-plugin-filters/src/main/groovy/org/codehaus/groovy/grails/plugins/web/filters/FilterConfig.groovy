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
package org.codehaus.groovy.grails.plugins.web.filters

import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.plugins.web.api.ControllersApi
import org.codehaus.groovy.grails.web.servlet.FlashScope
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.context.ApplicationContext
import org.springframework.validation.Errors
import org.springframework.web.servlet.ModelAndView

/**
 * @author mike
 * @author Graeme Rocher
 */
class FilterConfig extends ControllersApi {
    String name
    Map scope
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

    /**
     * Redirects attempt to access an 'errors' property, so we provide
     * one here with a null value.
     */
    def errors = null

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
    def propertyMissing(String propertyName) {
        // Delegate to the parent definition if it has this property.
        if (wiredFiltersDefinition.metaClass.hasProperty(wiredFiltersDefinition, propertyName)) {
            def getterName = GrailsClassUtils.getGetterName(propertyName)
            metaClass."$getterName" = {-> delegate.wiredFiltersDefinition.getProperty(propertyName) }
            return wiredFiltersDefinition."$propertyName"
        }

        throw new MissingPropertyException(propertyName, filtersDefinition.getClass())
    }

    def getWiredFiltersDefinition() {
        final webRequest = GrailsWebRequest.lookup()
        final grailsFilter = webRequest ? grailsApplication.getArtefact(FiltersConfigArtefactHandler.TYPE, filtersDefinition.class.name) : null
        if (grailsFilter) {
            applicationContext.getBean(grailsFilter.fullName)
        }
        else {
            return filtersDefinition
        }
    }

    /**
     * When the filter does not have a particular method, it passes
     * the call on to the filter definition class.
     */
    def methodMissing(String methodName, args) {
        // Delegate to the parent definition if it has this method.
        List<MetaMethod> respondsTo = filtersDefinition.metaClass.respondsTo(filtersDefinition, methodName, args)
        if (respondsTo) {
            // Use DelegateMetaMethod to proxy calls to actual MetaMethod for subsequent calls to this method
            DelegateMetaMethod dmm=new DelegateMetaMethod(respondsTo[0], FilterConfigDelegateMetaMethodTargetStrategy.instance)
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
                    "Invalid filter definition in ${wiredFiltersDefinition.getClass().name} - trying "
                    + "to call method '${methodName}' outside of an interceptor.")
        }

        // The required method was not found on the parent filter definition either.
        throw new MissingMethodException(methodName, wiredFiltersDefinition.getClass(), args)
    }

    String toString() {"FilterConfig[$name, scope=$scope]"}

    String getActionUri() {
        return super.getActionUri(this)
    }

    String getControllerUri() {
        return super.getControllerUri(this)
    }

    String getTemplateUri(String name) {
        return super.getTemplateUri(this, name)
    }

    String getViewUri(String name) {
        return super.getViewUri(this, name)
    }

    void setErrors(Errors errors) {
        super.setErrors(this, errors)
    }

    Errors getErrors() {
        return super.getErrors(this)
    }

    Map getChainModel() {
        return super.getChainModel(this)
    }

    boolean hasErrors() {
        return super.hasErrors(this)
    }

    Object redirect(Map args) {
        return super.redirect(this, args)
    }

    Object chain(Map args) {
        return super.chain(this, args)
    }

    Object render(Object o) {
        return super.render(this, o)
    }

    Object render(String txt) {
        return super.render(this, txt)
    }

    Object render(Map args) {
        return super.render(this, args)
    }

    Object render(Closure c) {
        return super.render(this, c)
    }

    Object render(Map args, Closure c) {
        return super.render(this, args, c)
    }

    Object bindData(Object target, Object args) {
        return super.bindData(this, target, args)
    }

    Object bindData(Object target, Object args, List disallowed) {
        return super.bindData(this, target, args, disallowed)
    }

    Object bindData(Object target, Object args, List disallowed, String filter) {
        return super.bindData(this, target, args, disallowed, filter)
    }

    Object bindData(Object target, Object args, Map includeExclude) {
        return super.bindData(this, target, args, includeExclude)
    }

    Object bindData(Object target, Object args, Map includeExclude, String filter) {
        return super.bindData(this, target, args, includeExclude, filter)
    }

    Object bindData(Object target, Object args, String filter) {
        return super.bindData(this, target, args, filter)
    }

    void header(String headerName, Object headerValue) {
        super.header(this, headerName, headerValue)
    }

    Object withForm(Closure callable) {
        return super.withForm(this, callable)
    }

    String forward(Map params) {
        return super.forward(this, params)
    }

    GrailsParameterMap getParams() {
        return super.getParams(this)
    }

    FlashScope getFlash() {
        return super.getFlash(this)
    }

    HttpSession getSession() {
        return super.getSession(this)
    }

    HttpServletRequest getRequest() {
        return super.getRequest(this)
    }

    ServletContext getServletContext() {
        return super.getServletContext(this)
    }

    HttpServletResponse getResponse() {
        return super.getResponse(this)
    }

    GrailsApplicationAttributes getGrailsAttributes() {
        return super.getGrailsAttributes(this)
    }

    GrailsApplication getGrailsApplication() {
        return super.getGrailsApplication(this)
    }

    ApplicationContext getApplicationContext() {
        return super.getApplicationContext(this)
    }

    String getActionName() {
        return super.getActionName(this)
    }

    String getControllerName() {
        return super.getControllerName(this)
    }

    GrailsWebRequest getWebRequest() {
        return super.getWebRequest(this)
    }

    String getPluginContextPath() {
        return super.getPluginContextPath(this)
    }
}
