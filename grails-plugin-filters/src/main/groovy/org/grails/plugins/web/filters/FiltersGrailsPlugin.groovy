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

import grails.plugins.Plugin
import grails.util.GrailsUtil
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Commons
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import grails.core.GrailsApplication
import grails.core.GrailsClass
import grails.core.support.GrailsApplicationAware
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Configures Filters.
 *
 * @author Mike
 * @author Graeme Rocher
 *
 * @since 1.0
 */
@Commons
class FiltersGrailsPlugin extends Plugin implements GrailsApplicationAware, ApplicationContextAware{

    private static final String TYPE = FiltersConfigArtefactHandler.TYPE

    def version = GrailsUtil.getGrailsVersion()
    def dependsOn = [controllers:version]
    def artefacts = [FiltersConfigArtefactHandler]
    def watchedResources = "file:./grails-app/conf/**/*Filters.groovy"


    @Override
    Closure doWithSpring() {{->
        filterInterceptor(CompositeInterceptor)
        def app = grailsApplication
        for (filter in app.getArtefacts(TYPE)) {
            "${filter.fullName}Class"(MethodInvokingFactoryBean) {
                targetObject = app
                targetMethod = "getArtefact"
                arguments = [TYPE, filter.fullName]
            }
            "${filter.fullName}"(filter.clazz) { bean ->
                bean.singleton = true
                bean.autowire = "byName"
            }
        }
    }}

    @Override
    @CompileStatic
    void doWithApplicationContext() {
        reloadFilters(grailsApplication, applicationContext)
    }

    @Override
    void onChange(Map<String, Object> event) {
        log.debug("onChange: $event")

        // Get the new or modified filter and (re-)register the associated beans
        def app = grailsApplication
        def newFilter = app.addArtefact(TYPE, (Class)event.source)

        beans {
            "${newFilter.fullName}Class"(MethodInvokingFactoryBean) {
                targetObject = app
                targetMethod = "getArtefact"
                arguments = [TYPE, newFilter.fullName]
            }
            "${newFilter.fullName}"(newFilter.clazz) { bean ->
                bean.singleton = true
                bean.autowire = "byName"
            }
        }
        FiltersGrailsPlugin.reloadFilters(grailsApplication, applicationContext)
    }

    @CompileStatic
    static void reloadFilters(GrailsApplication application, ApplicationContext applicationContext) {

        log.info "reloadFilters"
        GrailsFiltersClass[] filterConfigs = (GrailsFiltersClass[])application.getArtefacts(TYPE)
        def handlers = []

        List<GrailsFiltersClass> sortedFilterConfigs = [] // the new ordered filter list
        List<GrailsFiltersClass> list = new ArrayList<GrailsFiltersClass>(Arrays.asList(filterConfigs))
        def addedDeps = [:]

        while (list) {
            int filtersAdded = 0

            log.debug("Current filter order is '${filterConfigs.join(",")}'")

            for (Iterator<GrailsFiltersClass> iter = list.iterator(); iter.hasNext();) {
                GrailsFiltersClass c = iter.next()
                def bean = applicationContext.getBean(c.fullName)
                log.debug("Processing filter '${bean.getClass().name}'")

                List<Class> dependsOn = evaluateDependsOn(bean)


                if (dependsOn != null) {
                    log.debug("  depends on '${dependsOn.join(",")}'")
                    // check dependencies to see if all the filters it depends on are already in the list
                    log.debug("  Checking filter '${bean.getClass().name}' dependencies (${dependsOn.size()})")

                    boolean failedDep = false
                    for (dep in dependsOn) {
                        log.debug("  Checking filter '${bean.getClass().name}' dependencies: ${dep.name}")
                        //if (sortedFilterConfigs.find{def b = applicationContext.getBean(it.fullName); b.class == dep} == null) {
                        if (!addedDeps.containsKey(dep)) {
                            // dep not in the list yet, we need to skip adding this to the list for now
                            log.debug("  Skipped Filter '${bean.getClass().name}', since dependency '${dep.name}' not yet added")
                            failedDep = true
                            break
                        } else {
                            log.debug("  Filter '${bean.getClass().name}' dependency '${dep.name}' already added")
                        }
                    }

                    if (failedDep) {
                        // move on to next dependency
                        continue
                    }
                }

                log.debug("  Adding filter '${bean.getClass().name}', since all dependencies have been added")
                sortedFilterConfigs.add(c)
                addedDeps.put(bean.getClass(), null)
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
                    def bean = applicationContext.getBean(c.fullName)

                    // display this as a cyclical dep
                    log.warn("::   Filter ${bean.getClass().name}")
                    if (bean.metaClass.hasProperty(bean, "dependsOn")) {
                        def dependsOn = evaluateDependsOn(bean)
                        log.warn("  depends on '${dependsOn.join(",")}'")
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
        for (GrailsFiltersClass c in sortedFilterConfigs) {
            def filterClass = applicationContext.getBean("${c.fullName}Class",GrailsFiltersClass)
            if (filterClass == null) {
                continue
            }

            def bean = applicationContext.getBean(c.fullName)
            for (filterConfig in filterClass.getConfigs(bean)) {
                applicationContext.autowireCapableBeanFactory.autowireBeanProperties(filterConfig, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)
                def handlerAdapter = new FilterToHandlerAdapter(filterConfig: filterConfig, configClass: bean)
                handlerAdapter.afterPropertiesSet()
                handlers << handlerAdapter
                log.debug("  $handlerAdapter")
            }
        }

        applicationContext.getBean('filterInterceptor', CompositeInterceptor).handlers = handlers
    }

    @CompileDynamic
    protected static List<Class> evaluateDependsOn(bean) {
        List dependsOn = null
        if (bean.metaClass.hasProperty(bean, "dependsOn")) {
            dependsOn = bean.dependsOn
        }
        dependsOn
    }
}
