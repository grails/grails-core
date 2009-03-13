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
package grails.test

import grails.util.GrailsWebUtil
import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin
import org.codehaus.groovy.grails.plugins.web.mapping.UrlMappingsGrailsPlugin
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingEvaluator
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingsHolder
import org.codehaus.groovy.grails.web.multipart.ContentLengthAwareCommonsMultipartResolver
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.DispatcherServlet
import org.codehaus.groovy.grails.commons.GrailsControllerClass
import org.springframework.web.context.ServletContextAware
import javax.servlet.ServletContext

/**
 * @author Luke Daley
 */
class GrailsUrlMappingsTestCase extends GroovyTestCase {
    def webRequest
    def mappingsHolder
    def controllers

    def assertionKeys = ["controller", "action", "view"]

    def patternResolver = new PathMatchingResourcePatternResolver()
    def classLoader = new GroovyClassLoader()
    def mappingEvaluator
    def grailsApplication

    def createMappingsHolder() {
        new DefaultUrlMappingsHolder(
                this.urlMappingEvaluatees.collect {
                    mappingEvaluator.evaluateMappings((it instanceof Closure) ? it : GrailsClassUtils.getStaticPropertyValue(it, "mappings"))
                }.flatten()
        )
    }

    def createControllerMap() {
        controllers = [:]
        grailsApplication.controllerClasses.each { GrailsControllerClass cc ->
            controllers[cc.logicalPropertyName] = cc.newInstance()
        }
        controllers
    }

    def getUrlMappingEvaluatees() {
        def m = GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(this, "mappings")
        if (m == null) {
            return grailsApplication.urlMappingsClasses*.clazz
        } else if (mappings instanceof Closure || mappings instanceof Class) {
            return [mappings]
        } else {
            return mappings
        }
    }

    void setUp() {
        webRequest = RequestContextHolder.currentRequestAttributes()
        mappingEvaluator = new DefaultUrlMappingEvaluator(webRequest.servletContext)
        mappingsHolder = createMappingsHolder()
        controllers = createControllerMap()
    }

    void tearDown() {
        RequestContextHolder.resetRequestAttributes()
    }

    def getActions(controller) {
        def instance = controllers[controller]
        instance.class.declaredFields.findAll {
            instance."${it.name}" instanceof Closure
        }.collect {
            StringUtils.substringBeforeLast(it.name, "Flow")
        }
    }

    def getDefaultAction(controllerName) {
        def controller = controllers[controllerName]
        (controller.class.declaredFields.find { it.name == "defaultAction" }) ? controller.defaultAction : "index"
    }

    def assertController(controller, url) {
        if (!controllers.containsKey(controller)) {
            throw new IllegalArgumentException("Url mapping assertion for '$url' failed, '$controller' is not a valid controller")
        }
    }

    def assertAction(controller, action, url) {
        if (!getActions(controller).contains(action)) {
            throw new IllegalArgumentException("Url mapping assertion for '$url' failed, '$action' is not a valid action of controller '$controller'")
        }
    }

    def assertView(controller, view, url) {
        def pathPattern = "grails-app/views/" + ((controller) ? "$controller/" : "") + "${view}.*"
        if (!patternResolver.getResources(pathPattern)) {
            throw new IllegalArgumentException(
                    (controller) ? "Url mapping assertion for '$url' failed, '$view' is not a valid view of controller '$controller'" : "Url mapping assertion for '$url' failed, '$view' is not a valid view"
            )
        }
    }

    void assertUrlMapping(assertions, url) {
        assertUrlMapping(assertions, url, null);
    }

    void assertUrlMapping(assertions, url, paramAssertions) {
        assertForwardUrlMapping(assertions, url, paramAssertions)
        if (assertions.controller && !(url instanceof Integer)) {
            assertReverseUrlMapping(assertions, url, paramAssertions)
        }
    }


    void assertForwardUrlMapping(assertions, url) {
        assertForwardUrlMapping(assertions, url, null)
    }

    void assertForwardUrlMapping(assertions, url, paramAssertions) {

        if (assertions.action && !assertions.controller) {
            throw new IllegalArgumentException("Cannot assert action for url mapping without asserting controller")
        }

        if (assertions.controller) assertController(assertions.controller, url)
        if (assertions.action) assertAction(assertions.controller, assertions.action, url)
        if (assertions.view) assertView(assertions.controller, assertions.view, url)

        def mappingInfos
        if (url instanceof Integer) {
            mappingInfos = []
            def mapping = mappingsHolder.matchStatusCode(url)
            if (mapping) mappingInfos << mapping
        } else {
            mappingInfos = mappingsHolder.matchAll(url)
        }

        if (mappingInfos.size() == 0) throw new IllegalArgumentException("url '$url' did not match any mappings")

        mappingInfos.find {mapping ->
            mapping.configure(webRequest)
            for(key in assertionKeys) {
                if (assertions.containsKey(key)) {
                    def expected = assertions[key]
                    def actual = mapping."${key}Name"

                    // if this is not a match and there are still more potential matches try the next one
                    if(!controllers.containsKey(actual) && mappingInfos.size() > 1) return

                    if (key == "view") {
                        if (actual[0] == "/") actual = actual.substring(1)
                        if (expected[0] == "/") expected = expected.substring(1)
                    }
                    if (key == "action" && actual == null) actual = getDefaultAction(assertions.controller)

                    assertEquals("Url mapping $key assertion for '$url' failed", expected, actual)
                }
            }
            if (paramAssertions) {
                def params = [:]
                paramAssertions.delegate = params
                paramAssertions.resolveStrategy = Closure.DELEGATE_ONLY
                paramAssertions.call()
                params.each {name, value ->
                    assertEquals("Url mapping '$name' parameter assertion for '$url' failed", value.toString(), mapping.params[name])
                }
            }
            true
        }
    }

    void assertReverseUrlMapping(assertions, url) {
        assertReverseUrlMapping(assertions, url, null)
    }

    void assertReverseUrlMapping(assertions, url, paramAssertions) {
        def controller = assertions.controller
        def action = assertions.action
        def params = [:]
        if (paramAssertions) {
            paramAssertions.delegate = params
            paramAssertions.resolveStrategy = Closure.DELEGATE_ONLY
            paramAssertions.call()
        }
        def urlCreator = mappingsHolder.getReverseMapping(controller, action, params)
        assertNotNull("could not create reverse mapping of '$url' for {controller = $controller, action = $action, params = $params}", urlCreator)
        def createdUrl = urlCreator.createRelativeURL(controller, action, params, "UTF-8")
        assertEquals("reverse mapping assertion for {controller = $controller, action = $action, params = $params}", url, createdUrl)
    }
}
