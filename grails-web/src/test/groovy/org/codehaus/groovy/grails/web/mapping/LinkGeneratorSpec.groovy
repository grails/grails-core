package org.codehaus.groovy.grails.web.mapping

import spock.lang.Specification
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
import org.codehaus.groovy.grails.plugins.CoreGrailsPlugin
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication

/**
 * Tests for the {@link DefaultLinkGenerator} class
 */
class LinkGeneratorSpec extends Specification {

    def baseUrl = "http://myserver.com/foo"
    def context = "/bar"
    def resource = null
    
    def plugins
    
    def mainCssResource = [dir:'css', file:'main.css']

    protected getGenerator() {
        def generator = new DefaultLinkGenerator(baseUrl, context)
        if (plugins) {
            def pluginManager = new DefaultGrailsPluginManager([plugins] as Class[], new DefaultGrailsApplication())
             pluginManager.loadPlugins()
             generator.pluginManager = pluginManager
        }
    }
    
    protected getLink() {
        generator.resource(resource)
    }
    
    def "absolute links contains the base url and context"() {
        when:
            resource = mainCssResource + [absolute:true]

        then:
            link == "$baseUrl/$context/$resource.dir/$resource.file"
    }

    def "relative links contain the context"() {
        when:
            resource = mainCssResource

        then:
            link == "/$context/$resource.dir/$resource.file"
    }

    def "default to absolute links when no context path is specified"() {
        given:
            context = null

        when:
            resource = mainCssResource

        then:
            link == "$baseUrl/$resource.dir/$resource.file"
    }

    def "plugin paths are resolved with the plugin attribute"() {
        given:
            plugins = [CoreGrailsPlugin]

        when:
            resource = mainCssResource + [plugin: 'core']

        then:
            link == "/$context/plugins/core-Unknown/$resource.dir/$resource.file"
    }

    def "link contains given explicit context path"() {
        given:
            def customContextPath = "/test"
            
        when:
            resource = mainCssResource + [contextPath: customContextPath]

        then:
            link == "/$customContextPath/$resource.dir/$resource.file"
    }

}
