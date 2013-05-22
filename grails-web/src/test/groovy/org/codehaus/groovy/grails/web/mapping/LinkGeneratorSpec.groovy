package org.codehaus.groovy.grails.web.mapping

import grails.util.GrailsWebUtil
import grails.web.CamelCaseUrlConverter

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.plugins.CoreGrailsPlugin
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder

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
            link == '/bar/'

        when:
            linkParams.uri = ''

        then:
            link == '/bar'
    }

    def "Test create relative link with custom context"() {
        when: "No custom context path specified"
            linkParams.controller = 'one'
            linkParams.action = 'two'

        then: "The default is used"
            link == '/bar/one/two'

        when: "A custom context path is specified"
            linkParams.contextPath = '/different'
            linkParams.controller = 'one'
            linkParams.action = 'two'

        then: "The custom context path is used"
            link == '/different/one/two'

       when: "A blank context path is specified"
            linkParams.contextPath = ''
            linkParams.controller = 'one'
            linkParams.action = 'two'

        then: "No context path is used"
            link == '/one/two'
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
            link == "$customContextPath/$resource.dir/$resource.file"
    }

    def "link has no context path if blank context supplied"() {
        given:
            def customContextPath = ""

        when:
            resource = mainCssResource + [contextPath: customContextPath]

        then:
            link == "/$resource.dir/$resource.file"
    }

    def "test absolute links created from request scheme"() {

        given:
            final webRequest = GrailsWebUtil.bindMockWebRequest()
            MockHttpServletRequest request = webRequest.currentRequest

        when:
            baseUrl = null
            resource = mainCssResource + [absolute:true]
            webRequest.baseUrl = null
        then:
            link == "http://localhost/$resource.dir/$resource.file"

        when:
            request.serverPort = 8081
            webRequest.baseUrl = null
        then:
            link == "http://localhost:8081/$resource.dir/$resource.file"

        when:
            request.contextPath = "/blah"
            request.serverPort = 8081
            webRequest.baseUrl = null
        then:
            link == "http://localhost:8081/blah/$resource.dir/$resource.file"
    }

    def "caching should take request Host header, scheme and port in to account"() {

        given:
            final webRequest = GrailsWebUtil.bindMockWebRequest()
            MockHttpServletRequest request = webRequest.currentRequest
            baseUrl = null
            def cachingGenerator = getGenerator(true)

        when:
            resource = mainCssResource + [absolute:true]
            def cachedlink = cachingGenerator.resource(resource)

        then:
            cachedlink == "http://localhost/$resource.dir/$resource.file"

        when:
            request.serverName = "some.other.host"
            request.scheme = "https"
            request.serverPort = 443
            webRequest.baseUrl = null
            cachedlink = cachingGenerator.resource(resource)
        then:
            cachedlink == "https://some.other.host/$resource.dir/$resource.file"

        when:
            request.serverName = "localhost"
            request.scheme = "http"
            request.serverPort = 8081
            webRequest.baseUrl = null
            cachedlink = cachingGenerator.resource(resource)
        then:
            cachedlink == "http://localhost:8081/$resource.dir/$resource.file"

        when:
            request.contextPath = "/blah"
            request.serverPort = 8081
            webRequest.baseUrl = null
            cachedlink = cachingGenerator.resource(resource)
        then:
            cachedlink == "http://localhost:8081/blah/$resource.dir/$resource.file"
    }

    void cleanup() {
        RequestContextHolder.setRequestAttributes(null)
    }

    protected getGenerator(boolean cache=false) {
        def generator = cache ? new CachingLinkGenerator(baseUrl, context) : new DefaultLinkGenerator(baseUrl, context)
        final callable = { String controller, String action, String namespace, String pluginName, String httpMethod, Map params ->
            [createRelativeURL: { String c, String a, Map parameterValues, String encoding, String fragment ->
                "/$controller/$action".toString()
            }] as UrlCreator
        }
        generator.grailsUrlConverter = new CamelCaseUrlConverter()
        def urlMappingsHolder = [getReverseMapping: callable,getReverseMappingNoDefault: callable] as UrlMappingsHolder

        generator.urlMappingsHolder = urlMappingsHolder
        if (pluginManager) {
            generator.pluginManager = pluginManager
        }
        generator
    }

    protected getLink() {
        if (resource != null) {
            getGenerator().resource(resource)
        }
        else {
            getGenerator().link(linkParams)
        }
    }

    protected getCachedLink() {
        if (resource != null) {
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
