package org.codehaus.groovy.grails.web.mapping

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.plugins.CoreGrailsPlugin
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
import spock.lang.Specification

/**
 * Tests for the {@link DefaultLinkGenerator} class
 */
class LinkGeneratorSpec extends Specification {

    def baseUrl = "http://myserver.com/foo"
    def context = "/bar"
    def resource = null
    def linkParams = [:]
    def pluginManager
    
    def mainCssResource = [dir:'css', file:'main.css']


    def "Test create link with root URI"() {
        when:
            linkParams.uri = '/'

        then:
            link == '/'
    }

    def "absolute links contains the base url and context when cached"() {
        when:
            resource = mainCssResource + [absolute:true]

        then:
            cachedLink == "$baseUrl/$resource.dir/$resource.file"
            cachedLink == "$baseUrl/$resource.dir/$resource.file"
    }


    def "absolute links contains the base url and context"() {
        when:
            resource = mainCssResource + [absolute:true]

        then:
            link == "$baseUrl/$resource.dir/$resource.file"
    }

    def "relative links contain the context"() {
        when:
            resource = mainCssResource

        then:
            link == "$context/$resource.dir/$resource.file"
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
        
        and:
            def pluginName = "core"
            def pluginVersion = pluginManager.getGrailsPlugin(pluginName).version
            
        when:
            resource = mainCssResource + [plugin: pluginName]

        then:
            link == "$context/plugins/$pluginName-$pluginVersion/$resource.dir/$resource.file"
    }

    def "link contains given explicit context path"() {
        given:
            def customContextPath = "/test"
            
        when:
            resource = mainCssResource + [contextPath: customContextPath]

        then:
            link == "$context$customContextPath/$resource.dir/$resource.file"
    }


    protected getGenerator(boolean cache=false) {
        def generator = cache ? new CachingLinkGenerator(baseUrl, context) : new DefaultLinkGenerator(baseUrl, context)
        if (pluginManager) {
            generator.pluginManager = pluginManager
        }
        generator
    }

    protected getLink() {
        if(resource != null) {
            getGenerator().resource(resource)
        }
        else {
            getGenerator().link(linkParams)
        }
    }

    protected getCachedLink() {
        if(resource != null) {
            getGenerator(true).resource(resource)
        }
        else {
            getGenerator(true).link(linkParams)
        }

    }

    protected setPlugins(List<Class> pluginClasses) {
        pluginManager = new DefaultGrailsPluginManager(pluginClasses as Class[], new DefaultGrailsApplication())
        pluginManager.loadPlugins()
    }
}
