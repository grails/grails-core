package org.codehaus.groovy.grails.web.mapping

import spock.lang.Specification
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
import org.codehaus.groovy.grails.plugins.CoreGrailsPlugin
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication

/**
 * Tests for the {@link DefaultLinkGenerator} class
 */
class LinkGeneratorSpec extends Specification{


    def "Check that absolute links are generated correctly"() {
        given:
            def generator = new DefaultLinkGenerator("http://myserver.com/foo", "/bar")

        when:"A link is generated with absolute:true"
            def link = generator.resource(dir:'css', file:'main.css', absolute:true)

        then:"The link contains the absolute path taken from the server absolute URL"
            link == 'http://myserver.com/foo/css/main.css'
    }

    def "Check that relative links contain the contextpath"() {
        given:
            def generator = new DefaultLinkGenerator("http://myserver.com/foo", "/bar")

        when:"A link is generated without absolute:true"
            def link = generator.resource(dir:'css', file:'main.css')

        then:"A link is generated using the context path"
            link == '/bar/css/main.css'
    }

    def "Check that we default to absolute links when no context path is specified"() {
        given:
            def generator = new DefaultLinkGenerator("http://myserver.com/foo", null)

        when:"A relative link is generated and the generator has no context path"
            def link = generator.resource(dir:'css', file:'main.css')

        then:"The absolute server URL is used to calculate the path"
            link == 'http://myserver.com/foo/css/main.css'
    }

    def "Check that plugin paths are resolve with the plugin attribute"() {
        given:
            def generator = new DefaultLinkGenerator("http://myserver.com/foo", "/bar")
            def pluginManager = new DefaultGrailsPluginManager([CoreGrailsPlugin.class] as Class[], new DefaultGrailsApplication())
            pluginManager.loadPlugins()
            generator.pluginManager = pluginManager

        when:"A relative link is created with the plugin attribute"
            def link = generator.resource(dir:'css', file:'main.css', plugin:'core')

        then:"The correct relative path to the plugin resource is calculated"
            link == '/bar/plugins/core-Unknown/css/main.css'

    }

    def "Check that the correct relative link is generated when using an explicit context path"() {
        given:
            def generator = new DefaultLinkGenerator("http://myserver.com/foo", '/bar')

        when:"A relative link is generated and the generator has no context path"
            def link = generator.resource(dir:'css', file:'main.css', contextPath: '/test')

        then:"The absolute server URL is used to calculate the path"
            link == '/test/css/main.css'
    }

}
