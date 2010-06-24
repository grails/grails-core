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

import grails.util.GrailsUtil

import org.apache.commons.logging.LogFactory

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass
import org.codehaus.groovy.grails.plugins.web.filters.CompositeInterceptor
import org.codehaus.groovy.grails.plugins.web.filters.FiltersConfigArtefactHandler
import org.codehaus.groovy.grails.plugins.web.filters.FilterToHandlerAdapter
import org.codehaus.groovy.grails.web.metaclass.RedirectDynamicMethod
import org.codehaus.groovy.grails.web.metaclass.RenderDynamicMethod
import org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils

import org.springframework.beans.factory.config.MethodInvokingFactoryBean

/**
 * @author Mike
 * @author Graeme Rocher
 *
 * @since 1.0
 */
class FiltersGrailsPlugin {

    private static final TYPE = FiltersConfigArtefactHandler.TYPE

    def version = GrailsUtil.getGrailsVersion()
    def dependsOn = [:]
    def artefacts = [ FiltersConfigArtefactHandler ]
    def watchedResources = "file:./grails-app/conf/**/*Filters.groovy"
    def log = LogFactory.getLog(FiltersGrailsPlugin)

    static final BEANS = { GrailsClass filter ->
        "${filter.fullName}Class"(MethodInvokingFactoryBean) {
            targetObject = ref("grailsApplication", true)
            targetMethod = "getArtefact"
            arguments = [TYPE, filter.fullName]
        }
        "${filter.fullName}"(filter.clazz) { bean ->
            bean.singleton = true
            bean.autowire = "byName"
        }
    }

    def doWithSpring = {
        filterInterceptor(CompositeInterceptor)

        for(filter in application.getArtefacts(TYPE)) {
            def callable = BEANS.curry(filter)
            callable.delegate = delegate
            callable.call()
        }
    }

    def doWithDynamicMethods = { applicationContext ->
        def mc = FilterConfig.metaClass

        // Add the standard dynamic properties for web requests to all
        // the filters, i.e. 'params', 'flash', 'request', etc.
        WebMetaUtils.registerCommonWebProperties(mc, application)

        // Also make the application context available.
        mc.getApplicationContext = {-> applicationContext }

        // Add redirect and render methods (copy and pasted from the
        // controllers plugin).
        def redirect = new RedirectDynamicMethod(applicationContext)
        def render = new RenderDynamicMethod()

        mc.redirect = {Map args ->
            redirect.invoke(delegate, "redirect", args)
        }

        mc.render = {Object o ->
            render.invoke(delegate, "render", [o?.inspect()] as Object[])
        }

        mc.render = {String txt ->
            render.invoke(delegate, "render", [txt] as Object[])
        }

        mc.render = {Map args ->
            render.invoke(delegate, "render", [args] as Object[])
        }

        mc.render = {Closure c ->
            render.invoke(delegate, "render", [c] as Object[])
        }

        mc.render = {Map args, Closure c ->
            render.invoke(delegate, "render", [args, c] as Object[])
        }
    }

    def doWithApplicationContext = { applicationContext ->
        reloadFilters(application, applicationContext)
    }

    def onChange = { event ->

        log.debug("onChange: $event")

        // Get the new or modified filter and (re-)register the associated beans
        def newFilter = event.application.addArtefact(TYPE, event.source)
        beans(BEANS.curry(newFilter)).registerBeans(event.ctx)
        reloadFilters(event.application, event.ctx)
    }

    private reloadFilters(GrailsApplication application, applicationContext) {

        log.info "reloadFilters"
        def filterConfigs = application.getArtefacts(TYPE)
        def handlers = []

        def sortedFilterConfigs = [] // the new ordered filter list
        def list = new ArrayList(Arrays.asList(filterConfigs))
        def addedDeps = [:]

        while (list.size() > 0) {
            def filtersAdded = 0;

            log.debug("Current filter order is '"+filterConfigs.join(",")+"'")

            for (Iterator iter = list.iterator(); iter.hasNext(); ) {
                def c = iter.next();
                def filterClass = applicationContext.getBean("${c.fullName}Class")
                def bean = applicationContext.getBean(c.fullName)
                log.debug("Processing filter '${bean.class.name}'")

                def dependsOn = null
                if (bean.metaClass.hasProperty(bean, "dependsOn")) {
                    dependsOn = bean.dependsOn
                    log.debug("  depends on '"+dependsOn.join(",")+"'")
                }
                
                if (dependsOn != null) {
                    // check dependencies to see if all the filters it depends on are already in the list
                    log.debug("  Checking filter '${bean.class.name}' dependencies (${dependsOn.size()})")

                    def failedDep = false;
                    for (dep in dependsOn) {
                        log.debug("  Checking filter '${bean.class.name}' dependencies: ${dep.name}")
                        //if (sortedFilterConfigs.find{def b = applicationContext.getBean(it.fullName); b.class == dep} == null) {
                        if (!addedDeps.containsKey(dep)) {
                            // dep not in the list yet, we need to skip adding this to the list for now
                            log.debug("  Skipped Filter '${bean.class.name}', since dependency '${dep.name}' not yet added")
                            failedDep = true
                            break
                        } else {
                            log.debug("  Filter '${bean.class.name}' dependency '${dep.name}' already added")
                        }
                    }

                    if (failedDep) {
                        // move on to next dependency
                        continue
                    }
                }

                log.debug("  Adding filter '${bean.class.name}', since all dependencies have been added")
                sortedFilterConfigs.add(c)
                addedDeps.put(bean.getClass(), null);
                iter.remove()
                filtersAdded++
            }

            // if we didn't add any filters this iteration, then we have a cyclical dep problem
            if (filtersAdded == 0) {
                // we have a cyclical dependency, warn the user and load in the order they appeared originally
                log.warn(":::::::::::::::::::::::::::::::::::::::::::::::")
                log.warn("::   Cyclical Filter dependencies detected   ::")
                log.warn("::   Continuing with original filter order   ::")
                log.warn(":::::::::::::::::::::::::::::::::::::::::::::::")
                for (c in list) {
                    def filterClass = applicationContext.getBean("${c.fullName}Class")
                    def bean = applicationContext.getBean(c.fullName)

                    // display this as a cyclical dep
                    log.warn("::   Filter "+bean.class.name)
                    def dependsOn = null
                    if (bean.metaClass.hasProperty(bean, "dependsOn")) {
                        dependsOn = bean.dependsOn
                        for (dep in dependsOn) {
                            log.warn("::    depends on "+dep.name)
                        }
                    } else {
                        // we should only have items left in the list with deps, so this should never happen
                        // but a wise man once said...check for true, false and otherwise...just in case
                        log.warn("::   Problem while resolving cyclical dependencies.")
                        log.warn("::   Unable to resolve dependency hierarchy.")
                    }
                    log.warn(":::::::::::::::::::::::::::::::::::::::::::::::")
                }
                break
            // if we have processed all the filters, we are done
            } else if (sortedFilterConfigs.size() == filterConfigs.size()) {
                log.debug("Filter dependency ordering complete")
                break
            }
        }
        
        // add the filter configs in dependency sorted order
        log.debug("Resulting handlers:")
        for (c in sortedFilterConfigs) {
            def filterClass = applicationContext.getBean("${c.fullName}Class")
            def bean = applicationContext.getBean(c.fullName)
            for (filterConfig in filterClass.getConfigs(bean)) {
                def handlerAdapter = new FilterToHandlerAdapter(filterConfig:filterConfig, configClass:bean)
                handlerAdapter.afterPropertiesSet()
                handlers <<  handlerAdapter
                log.debug("  $handlerAdapter")
            }
        }

        applicationContext.getBean('filterInterceptor').handlers = handlers
    }
}
