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
package grails.test.mixin.web

import junit.framework.AssertionFailedError
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsControllerClass
import org.codehaus.groovy.grails.commons.UrlMappingsArtefactHandler
import org.codehaus.groovy.grails.web.mapping.UrlMappingInfo
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolderFactoryBean
import org.codehaus.groovy.grails.web.mapping.filter.UrlMappingsFilter
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.codehaus.groovy.grails.web.util.WebUtils
import static junit.framework.Assert.assertEquals
import static junit.framework.Assert.assertNotNull

 /**
 * A mixin for testing URL mappings in Grails
 *
 * @author Luke Daley
 * @author Graeme Rocher
 *
 * @since 1.4
 */
class UrlMappingsUnitTestMixin extends ControllerUnitTestMixin{

    private assertionKeys = ["controller", "action", "view"]

    /**
     * Mocks specific URL mappings class
     *
     * @param urlMappingsClass The URL mappings class to mock
     * @return The UrlMappingsHolder class
     */
    UrlMappingsHolder mockUrlMappings(Class urlMappingsClass) {
        grailsApplication.addArtefact(UrlMappingsArtefactHandler.TYPE, urlMappingsClass)

        defineBeans {
            grailsUrlMappingsHolder(UrlMappingsHolderFactoryBean) {
                grailsApplication = grailsApplication
                servletContext = ControllerUnitTestMixin.servletContext
            }

        }

        getUrlMappingsHolder()
    }

    /**
     * @return The {@link UrlMappingsHolder} bean
     */
    UrlMappingsHolder getUrlMappingsHolder() {
        applicationContext.getBean("grailsUrlMappingsHolder", UrlMappingsHolder)
    }

    /**
     * Maps a URl and returns the appropriate controller instance
     *
     * @param uri The URI to map
     * @return The controller instance
     */
    def mapURI(String uri) {
        UrlMappingsHolder mappingsHolder = getUrlMappingsHolder()

        if (!UrlMappingsFilter.isUriExcluded(mappingsHolder, uri)) {
            UrlMappingInfo[] mappingInfos = mappingsHolder.matchAll(uri, request.method)
            for (UrlMappingInfo info in mappingInfos) {
                def backupParams = new HashMap(webRequest.params)
                info.configure(webRequest)

                webRequest.params.putAll(backupParams)
                if (info.viewName == null && info.URI == null) {
                    def controller = grailsApplication.getArtefactForFeature(ControllerArtefactHandler.TYPE, "${WebUtils.SLASH}${info.controllerName}${WebUtils.SLASH}${info.actionName ?: ''}");
                    if (controller != null) {
                        return applicationContext.getBean(controller.fullName)
                    }
                }
            }
        }
    }
    /**
     * asserts a controller exists for the specified name and url
     *
     * @param controller The controller name
     * @param url The url
     */
    void assertController(controller, url) {
        final controllerClass = getControllerClass(controller)
        if (!controllerClass) {
            throw new AssertionFailedError("Url mapping assertion for '$url' failed, '$controller' is not a valid controller")
        }
    }

    /**
     * Asserts an action exists for the specified controller name, action name and url
     *
     * @param controller The controller name
     * @param action The action name
     * @param url The URL
     */
    void assertAction(controller, action, url) {
        final controllerClass = getControllerClass(controller)

        if (!controllerClass?.mapsToURI("/$controller/$action")) {
            throw new AssertionFailedError("Url mapping assertion for '$url' failed, '$action' is not a valid action of controller '$controller'")
        }
    }

    /**
     * Asserts a view exists for the specified controller name, view name and url
     *
     * @param controller The controller name
     * @param view The view name
     * @param url The url
     */
    void assertView(controller, view, url) {
        def pathPattern =  ((controller) ? "$controller/" : "") + "${view}.gsp"
        if (!pathPattern.startsWith('/')) {
            pathPattern = "/$pathPattern"
        }
        GroovyPagesTemplateEngine templateEngine = applicationContext.getBean("groovyPagesTemplateEngine", GroovyPagesTemplateEngine)

        def t = templateEngine.createTemplate(pathPattern)
        if (!t) {
            throw new AssertionFailedError(
                (controller) ? "Url mapping assertion for '$url' failed, '$view' is not a valid view of controller '$controller'" : "Url mapping assertion for '$url' failed, '$view' is not a valid view")
        }
    }

    /**
     * Asserts a URL mapping maps to the controller and action specified by named parameters. Example:
     *
     * <pre>
     * <code>
     *           assertUrlMapping("/action1", controller: "grailsUrlMappingsTestCaseFake", action: "action1")
     * </code>
     * </pre>
     * @param assertions The assertions as named parameters
     * @param url The URL as a string
     */
    void assertUrlMapping(Map assertions, url) {
        assertUrlMapping(assertions, url, null);
    }

    /**
     * Asserts a URL mapping maps to the controller and action specified by named parameters. Example:
     *
     * <pre>
     * <code>
     *           assertUrlMapping("/action1", controller: "grailsUrlMappingsTestCaseFake", action: "action1")
     * </code>
     * </pre>
     * @param assertions The assertions as named parameters
     * @param url The URL as a string
     * @param paramAssertions The parameters to assert defined in the body of the closure
     */
    void assertUrlMapping(Map assertions, url, Closure paramAssertions) {
        assertForwardUrlMapping(assertions, url, paramAssertions)
        if (assertions.controller && !(url instanceof Integer)) {
            assertReverseUrlMapping(assertions, url, paramAssertions)
        }
    }

    void assertForwardUrlMapping(assertions, url) {
        assertForwardUrlMapping(assertions, url, null)
    }

    void assertForwardUrlMapping(assertions, url, paramAssertions) {

        UrlMappingsHolder mappingsHolder = applicationContext.getBean("grailsUrlMappingsHolder", UrlMappingsHolder)
        if (assertions.action && !assertions.controller) {
            throw new AssertionFailedError("Cannot assert action for url mapping without asserting controller")
        }

        if (assertions.controller) assertController(assertions.controller, url)
        if (assertions.action) assertAction(assertions.controller, assertions.action, url)
        if (assertions.view) assertView(assertions.controller, assertions.view, url)

        def mappingInfos
        if (url instanceof Integer) {
            mappingInfos = []
            def mapping = mappingsHolder.matchStatusCode(url)
            if (mapping) mappingInfos << mapping
        }
        else {
            mappingInfos = mappingsHolder.matchAll(url)
        }

        if (mappingInfos.size() == 0) throw new AssertionFailedError("url '$url' did not match any mappings")

        def mappingMatched = mappingInfos.any {mapping ->
            mapping.configure(webRequest)
            for (key in assertionKeys) {
                if (assertions.containsKey(key)) {
                    def expected = assertions[key]
                    def actual = mapping."${key}Name"

                    switch (key) {
                        case "controller":
                            if (actual && !getControllerClass(actual)) return false
                            break
                        case "view":
                            if (actual[0] == "/") actual = actual.substring(1)
                            if (expected[0] == "/") expected = expected.substring(1)
                            break
                        case "action":
                            if (key == "action" && actual == null) {
                                final controllerClass = getControllerClass(assertions.controller)
                                actual = controllerClass?.defaultAction
                            }
                            break
                    }

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
            return true
        }

        if (!mappingMatched) throw new IllegalArgumentException("url '$url' did not match any mappings")
    }

    /**
     * Asserts the given controller and action produce the given reverse URL mapping
     *
     * <pre>
     * <code>
     *           assertReverseUrlMapping("/action1", controller: "grailsUrlMappingsTestCaseFake", action: "action1")
     * </code>
     * </pre>
     * @param assertions The assertions as named parameters
     * @param url The URL as a string
     */
    void assertReverseUrlMapping(Map assertions, url) {
        assertReverseUrlMapping(assertions, url, null)
    }

    /**
     * Asserts the given controller and action produce the given reverse URL mapping
     *
     * <pre>
     * <code>
     *           assertReverseUrlMapping("/action1", controller: "grailsUrlMappingsTestCaseFake", action: "action1")
     * </code>
     * </pre>
     * @param assertions The assertions as named parameters
     * @param url The URL as a string
     * @param paramAssertions The parameters to assert defined in the body of the closure
     */
    void assertReverseUrlMapping(Map assertions, url, Closure paramAssertions) {
        UrlMappingsHolder mappingsHolder = applicationContext.getBean("grailsUrlMappingsHolder", UrlMappingsHolder)
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

    private GrailsControllerClass getControllerClass(controller) {
        return grailsApplication.getArtefactByLogicalPropertyName(ControllerArtefactHandler.TYPE, controller)
    }
}
