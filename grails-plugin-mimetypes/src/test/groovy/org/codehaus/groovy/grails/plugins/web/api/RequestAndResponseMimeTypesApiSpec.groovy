package org.codehaus.groovy.grails.plugins.web.api

import grails.util.GrailsWebUtil

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.metaclass.MetaClassEnhancer
import org.codehaus.groovy.grails.plugins.web.mimes.MimeTypesFactoryBean
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests for {@link RequestMimeTypesApi}
 */
@Ignore
class RequestAndResponseMimeTypesApiSpec extends Specification{
    def requestMimeTypesApiInstance
    def responseMimeTypesApiInstance
    def application
    
    void setup() {
        application = new DefaultGrailsApplication()
        application.config = testConfig
        responseMimeTypesApiInstance = responseMimeTypesApi
        requestMimeTypesApiInstance = requestMimeTypesApi
        registerRequestAndResponseMimeTypesApi()
    }
    
    void registerRequestAndResponseMimeTypesApi() {
        MetaClassEnhancer requestEnhancer = new MetaClassEnhancer()
        requestEnhancer.addApi requestMimeTypesApiInstance
        requestEnhancer.enhance HttpServletRequest.metaClass

        MetaClassEnhancer responseEnhancer = new MetaClassEnhancer()
        responseEnhancer.addApi responseMimeTypesApiInstance
        responseEnhancer.enhance HttpServletResponse.metaClass
    }
    
    void cleanup() {
        GroovySystem.metaClassRegistry.removeMetaClass(HttpServletRequest)
        GroovySystem.metaClassRegistry.removeMetaClass(MockHttpServletRequest)
        GroovySystem.metaClassRegistry.removeMetaClass(HttpServletResponse)
        GroovySystem.metaClassRegistry.removeMetaClass(MockHttpServletResponse)
    }

    void "Test format property is valid for CONTENT_TYPE header only"() {
        when: "The request CONTENT_TYPE header is 'text/xml'"
            final webRequest = GrailsWebUtil.bindMockWebRequest()
            def request = webRequest.currentRequest
            def response = webRequest.currentResponse
            request.contentType = "text/xml"

        then: "The request format should be 'xml'"
            requestMimeTypesApi.getFormat(request) == "xml"
            requestMimeTypesApi.getFormat(request) == "xml" // call twice to test cached value
            request.format == 'xml'
            response.format == 'all'
    }

    void "Test format property is valid for CONTENT_TYPE and Accept header"() {
        when: "The request CONTENT_TYPE header is 'text/xml'"
            final webRequest = GrailsWebUtil.bindMockWebRequest()
            MockHttpServletRequest request = webRequest.currentRequest
            def response = webRequest.currentResponse
            request.contentType = "text/xml"
            request.addHeader('Accept', 'text/json')

        then: "The request format should be 'xml'"
            requestMimeTypesApi.getFormat(request) == "xml"
            requestMimeTypesApi.getFormat(request) == "xml" // call twice to test cached value
            request.format == 'xml'
            response.format == 'json'
    }

   void "Test format property is valid for Accept header only"() {
        when: "The request CONTENT_TYPE header is 'text/xml'"
            final webRequest = GrailsWebUtil.bindMockWebRequest()
            MockHttpServletRequest request = webRequest.currentRequest
            def response = webRequest.currentResponse
            request.addHeader('Accept', 'text/json')

        then: "The request format should be 'xml'"
            requestMimeTypesApi.getFormat(request) == "all"
            requestMimeTypesApi.getFormat(request) == "all" // call twice to test cached value
            request.format == 'all'
            response.format == 'json'
    }

    void "Test withFormat method with CONTENT_TYPE header only"() {
        when: "The request CONTENT_TYPE header is 'text/xml' and withFormat is used"
            final webRequest = GrailsWebUtil.bindMockWebRequest()
            def request = webRequest.currentRequest
            def response = webRequest.currentResponse

            request.contentType = "text/xml"
            def requestResult = request.withFormat {
                html { "got html"}
                xml { "got xml"}
            }

            def responseResult = response.withFormat {
                html { 'got html' }
                xml { 'got xml' }
            }

        then: 'The xml closure is invoked'
            requestResult == 'got xml'
            responseResult == 'got html'

    }

    void "Test withFormat method with Accept header only"() {
        when: "The request Accept header is 'text/xml' and withFormat is used"
            final webRequest = GrailsWebUtil.bindMockWebRequest()
            def request = webRequest.currentRequest
            def response = webRequest.currentResponse

            request.addHeader('Accept', "text/xml")

            def requestResult = request.withFormat {
                html { "got html"}
                xml { "got xml"}

            }

            def responseResult = response.withFormat {
                html { 'got html' }
                xml { 'got xml' }
            }

        then: 'The xml closure is invoked'
            requestResult == 'got html'
            responseResult == 'got xml'

        when:"The Accept header is JSON and there is a catch-all"
            webRequest = GrailsWebUtil.bindMockWebRequest()
            request = webRequest.currentRequest
            response = webRequest.currentResponse
            request.addHeader('Accept', "application/json")
            responseResult = response.withFormat {
                html { 'got html' }
                xml { 'got xml' }
                '*' { 'got everything' }
            }
        then: 'The * closure is invoked'
            responseResult == 'got everything'

    }
    
    @Issue("GRAILS-10973")
    void "request.withFormat should choose wildcard choice when format == all"() {
        when:
            final webRequest = GrailsWebUtil.bindMockWebRequest()
            def request = webRequest.currentRequest
            def response = webRequest.currentResponse
            def requestResult = request.withFormat {
                    html { 'got html' }
                    xml { 'got xml' }
                    '*' { 'got everything' }
                }
        then: 'format is all'
            request.format == 'all'
        then: 'The * closure is invoked'
            requestResult == 'got everything'
    }

    void "Test withFormat returns first block if no format provided"() {
        when: "No Accept header, URI extension or format param"
        final webRequest = GrailsWebUtil.bindMockWebRequest()
        def request = webRequest.currentRequest
        def response = webRequest.currentResponse

        def responseResult = response.withFormat {
            json { 'got json' }
            xml { 'got xml' }
        }

        then: "The first withFormat block should be returned"
        responseResult == 'got json'
    }

    void "Test withFormat method when Accept header contains the all (*/*) and non-matching formats"() {
        setup: "The request Acept header is 'application/xml, text/csv, */*' and withFormat is used"
            final webRequest = GrailsWebUtil.bindMockWebRequest()
            def request = webRequest.currentRequest
            def response = webRequest.currentResponse
            request.addHeader('Accept', acceptHeader)
            def responseResult = response.withFormat {
                json { 'got json' }
                text { 'got text' }
                html { 'got html' }
            }

        expect:
            formatResponse == responseResult

        where:
            formatResponse  | acceptHeader
            null            | 'application/xml, text/csv'
            'got html'      | 'application/xml, text/html, */*'
            'got json'      | 'application/xml, */*, text/html'
            'got json'      | 'application/xml, text/csv, */*'

    }

    @Unroll
    void "Test withFormat method when Accept header and User-Agent #userAgent #additionalConfig #acceptHeader"() {
        setup:
            def config = getTestConfig()
            if(additionalConfig) {
                config.merge(new ConfigSlurper().parse(String.valueOf(additionalConfig)))
            }
            application.setConfig(config)
            println "$userAgent - $additionalConfig - ${application.flatConfig.get('grails.mime.disable.accept.header.userAgents')}"
            responseMimeTypesApiInstance.loadConfig()
            println "disableForUserAgents: ${responseMimeTypesApiInstance.disableForUserAgents}"
            final webRequest = GrailsWebUtil.bindMockWebRequest()
            def request = webRequest.currentRequest
            def response = webRequest.currentResponse
            request.addHeader('Accept', acceptHeader)
            request.addHeader('User-Agent', userAgent)
            def responseResult = response.withFormat {
                json { 'got json' }
                text { 'got text' }
                html { 'got html' }
            }

        expect:
            formatResponse == responseResult

        where:
            formatResponse  | acceptHeader                       | userAgent    | additionalConfig
            null            | 'application/xml, text/csv'        | 'Mozilla'    | ''
            'got html'      | 'application/xml, text/html, */*'  | 'Mozilla'    | ''
            'got json'      | 'application/xml, */*, text/html'  | 'Mozilla'    | ''
            'got json'      | 'application/xml, text/csv, */*'   | 'Mozilla'    | ''
            'got json'      | 'application/xml, text/html, */*'  | 'Trident'    | ''
            'got html'      | 'application/xml, text/html, */*'  | 'Trident'    | 'grails.mime.disable.accept.header.userAgents = []'
            'got html'      | 'application/xml, text/html, */*'  | 'Trident'    | 'grails.mime.disable.accept.header.userAgents = null'
    }

    
    private RequestMimeTypesApi getRequestMimeTypesApi() {
        def mimeTypesFactory = new MimeTypesFactoryBean()
        mimeTypesFactory.grailsApplication = application
        return new RequestMimeTypesApi(application, mimeTypesFactory.getObject())
    }

    private ResponseMimeTypesApi getResponseMimeTypesApi() {
        def mimeTypesFactory = new MimeTypesFactoryBean()
        mimeTypesFactory.grailsApplication = application
        return new ResponseMimeTypesApi(application, mimeTypesFactory.getObject())
    }

    String applicationConfigText = '''
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = true
grails.mime.types = [ 
                      all: '*/*',
                      html: ['text/html','application/xhtml+xml'],
                      xml: ['text/xml', 'application/xml'],
                      text: 'text/plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      csv: 'text/csv',
                      json: ['application/json','text/json'],
                      form: 'application/x-www-form-urlencoded',
                      multipartForm: 'multipart/form-data'
                    ]
'''

    private getTestConfig() {
        def s = new ConfigSlurper()
        s.parse(String.valueOf(applicationConfigText))
    }
}
