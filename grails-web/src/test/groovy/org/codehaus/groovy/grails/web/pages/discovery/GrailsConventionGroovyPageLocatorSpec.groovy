package org.codehaus.groovy.grails.web.pages.discovery

import spock.lang.Specification
import org.codehaus.groovy.grails.support.SimpleMapResourceLoader
import org.springframework.core.io.ByteArrayResource

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 6/30/11
 * Time: 5:37 PM
 * To change this template use File | Settings | File Templates.
 */
class GrailsConventionGroovyPageLocatorSpec extends Specification{
    SimpleMapResourceLoader resourceLoader = new SimpleMapResourceLoader()

    void "Test find view with controller and view name"() {
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
        locator.addResourceLoader(resourceLoader)
        return locator
    }
}
