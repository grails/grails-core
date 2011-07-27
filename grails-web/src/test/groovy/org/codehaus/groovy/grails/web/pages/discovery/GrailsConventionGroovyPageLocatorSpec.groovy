package org.codehaus.groovy.grails.web.pages.discovery

import spock.lang.Specification
import org.codehaus.groovy.grails.support.SimpleMapResourceLoader
import org.springframework.core.io.ByteArrayResource
import org.codehaus.groovy.grails.plugins.metadata.GrailsPlugin
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.plugins.CoreGrailsPlugin
import org.springframework.web.context.request.RequestContextHolder
import grails.util.GrailsWebUtil
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import grails.util.GrailsUtil

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 6/30/11
 * Time: 5:37 PM
 * To change this template use File | Settings | File Templates.
 */
class GrailsConventionGroovyPageLocatorSpec extends Specification{
    SimpleMapResourceLoader resourceLoader = new SimpleMapResourceLoader()

    void "Test find template with controller instance and view name"() {
        given: "a simple resource loader with a path to the view"
            resourceLoader.resources["/grails-app/views/test/_bar.gsp"] = new ByteArrayResource("contents".bytes)
        when: "The controller and template name is specified"
            def source = pageLocator.findTemplate(new TestController(), "bar")
        then: "the script source is found"
            source != null
            source.URI == '/test/_bar.gsp'

        when:"A non-existent template is queried"
            source = pageLocator.findTemplate(new TestController(), "notThere")
        then:"source is null"
            source == null

    }

    void "Test find view with controller instance and view name"() {
        given: "a simple resource loader with a path to the view"
            resourceLoader.resources["/grails-app/views/test/bar.gsp"] = new ByteArrayResource("contents".bytes)
        when: "The controller and view name is specified"
            def source = pageLocator.findView(new TestController(), "bar")
        then: "the script source is found"
            source != null
            source.URI == '/test/bar.gsp'

        when:"A non-existent view is queried"
            source = pageLocator.findView(new TestController(), "notThere")
        then:"source is null"
            source == null

    }

    void "Test find view with controller instance, view name and specified response format"() {
        setup:
            def webRequest = GrailsWebUtil.bindMockWebRequest()




        when: "The controller and view name is specified as well as a response format of xml"
            resourceLoader.resources["/grails-app/views/test/bar.xml.gsp"] = new ByteArrayResource("contents".bytes)
            resourceLoader.resources["/grails-app/views/test/bar.gsp"] = new ByteArrayResource("contents".bytes)

            webRequest.request.setAttribute(GrailsApplicationAttributes.RESPONSE_FORMAT, "xml")
            def source = pageLocator.findView(new TestController(), "bar")
        then: "the script source for the xml view is found"
            source != null
            source.URI == '/test/bar.xml.gsp'

        when: "no response format is specified"
            webRequest.request.removeAttribute(GrailsApplicationAttributes.RESPONSE_FORMAT)
            source = pageLocator.findView(new TestController(), "bar")

        then: "the default view is found"
            source != null
            source.URI == '/test/bar.gsp'


        when:"A non-existent view is queried"
            source = pageLocator.findView(new TestController(), "notThere")
        then:"source is null"
            source == null

        cleanup:
            RequestContextHolder.setRequestAttributes(null)

    }
    void "Test find view with controller instance and view name from plugin"() {
        given: "a valid path to a plugin view"
            resourceLoader.resources["/grails-app/views/plugins/core-${GrailsUtil.grailsVersion}/grails-app/views/bar.gsp"] = new ByteArrayResource("contents".bytes)
        when: "The controller from a plugin and view name is specified"
            def source = pageLocator.findView(new PluginController(), "bar")
        then: "the script source is found"
            source != null
            source.URI == "/plugins/core-${GrailsUtil.grailsVersion}/grails-app/views/bar.gsp"

        when:"A non-existent view is queried"
            source = pageLocator.findView(new PluginController(), "notThere")
        then:"source is null"
            source == null

    }

    void "Test find view with controller name and view name"() {
        given: "a simple resource loader with a path to the view"
            resourceLoader.resources["/grails-app/views/foo/bar.gsp"] = new ByteArrayResource("contents".bytes)
        when: "The controller and view name is specified"
            def source = pageLocator.findView("foo", "bar")
        then: "the script source is found"
            source != null
            source.URI == '/foo/bar.gsp'

        when:"A non-existent view is queried"
            source = pageLocator.findView("not", "there")
        then:"source is null"
            source == null

    }

    void "Test find view by path"() {
        given: "a simple resource loader with a path to the view"
            resourceLoader.resources["/grails-app/views/foo/bar.gsp"] = new ByteArrayResource("contents".bytes)
        when: "The controller and view name is specified"
            def source = pageLocator.findViewByPath("/foo/bar")
        then: "the script source is found"
            source != null
            source.URI == '/foo/bar.gsp'

        when:"A non-existent view is queried"
            source = pageLocator.findViewByPath("/not/there")
        then:"source is null"
            source == null

    }

    void "Test find template by controller and template name"() {
        given: "a simple resource loader with a path to the view"
            resourceLoader.resources["/grails-app/views/foo/_bar.gsp"] = new ByteArrayResource("contents".bytes)
        when: "The controller and view name is specified"
            def source = pageLocator.findTemplate("foo", "bar")
        then: "the script source is found"
            source != null
            source.URI == '/foo/_bar.gsp'

        when:"A non-existent view is queried"
            source = pageLocator.findTemplate("not", "there")
        then:"source is null"
            source == null

    }

    void "Test find template by path"() {
        given: "a simple resource loader with a path to the view"
            resourceLoader.resources["/grails-app/views/foo/_bar.gsp"] = new ByteArrayResource("contents".bytes)
        when: "The controller and view name is specified"
            def source = pageLocator.findTemplateByPath("/foo/bar")
        then: "the script source is found"
            source != null
            source.URI == '/foo/_bar.gsp'

        when:"A non-existent view is queried"
            source = pageLocator.findTemplateByPath("/not/there")
        then:"source is null"
            source == null

    }

    GrailsConventionGroovyPageLocator getPageLocator() {
        GrailsConventionGroovyPageLocator locator = new GrailsConventionGroovyPageLocator()
        def pluginManager = new DefaultGrailsPluginManager([CoreGrailsPlugin] as Class[], new DefaultGrailsApplication())
        pluginManager.loadPlugins()
        locator.pluginManager = pluginManager

        locator.addResourceLoader(resourceLoader)
        return locator
    }
}
class TestController {}

@GrailsPlugin(name="core", version="0.1")
class PluginController {}
