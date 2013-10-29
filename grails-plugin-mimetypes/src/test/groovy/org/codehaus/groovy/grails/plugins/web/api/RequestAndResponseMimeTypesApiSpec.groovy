package org.codehaus.groovy.grails.plugins.web.api

import grails.util.GrailsWebUtil
import javax.servlet.http.HttpServletRequest
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.metaclass.MetaClassEnhancer
import org.codehaus.groovy.grails.plugins.web.mimes.MimeTypesFactoryBean
import spock.lang.Specification
import javax.servlet.http.HttpServletResponse
import org.springframework.mock.web.MockHttpServletRequest

/**
 * Tests for {@link RequestMimeTypesApi}
 */
class RequestAndResponseMimeTypesApiSpec extends Specification{

    void setup() {
        MetaClassEnhancer requestEnhancer = new MetaClassEnhancer()
        requestEnhancer.addApi requestMimeTypesApi
        requestEnhancer.enhance HttpServletRequest.metaClass

        MetaClassEnhancer responseEnhancer = new MetaClassEnhancer()
        responseEnhancer.addApi responseMimeTypesAPi
        responseEnhancer.enhance HttpServletResponse.metaClass
    }

    void cleanup() {
        GroovySystem.metaClassRegistry.removeMetaClass(HttpServletRequest)
        GroovySystem.metaClassRegistry.removeMetaClass(HttpServletResponse)
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

    void "Test format property is valid for CONTENT_TYPE and ACCEPT header"() {
        when: "The request CONTENT_TYPE header is 'text/xml'"
            final webRequest = GrailsWebUtil.bindMockWebRequest()
            MockHttpServletRequest request = webRequest.currentRequest
            def response = webRequest.currentResponse
            request.contentType = "text/xml"
            request.addHeader('ACCEPT', 'text/json')

        then: "The request format should be 'xml'"
            requestMimeTypesApi.getFormat(request) == "xml"
            requestMimeTypesApi.getFormat(request) == "xml" // call twice to test cached value
            request.format == 'xml'
            response.format == 'json'
    }

   void "Test format property is valid for ACCEPT header only"() {
        when: "The request CONTENT_TYPE header is 'text/xml'"
            final webRequest = GrailsWebUtil.bindMockWebRequest()
            MockHttpServletRequest request = webRequest.currentRequest
            def response = webRequest.currentResponse
            request.addHeader('ACCEPT', 'text/json')

        then: "The request format should be 'xml'"
            requestMimeTypesApi.getFormat(request) == "html"
            requestMimeTypesApi.getFormat(request) == "html" // call twice to test cached value
            request.format == 'html'
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

    void "Test withFormat method with ACCEPT header only"() {
        when: "The request ACCEPT header is 'text/xml' and withFormat is used"
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

        when:"The ACCEPT header is JSON and there is a catch-all"
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

    private RequestMimeTypesApi getRequestMimeTypesApi() {
        final application = new DefaultGrailsApplication()
        application.config = config
        def mimeTypesFactory = new MimeTypesFactoryBean()
        mimeTypesFactory.grailsApplication = application

        return new RequestMimeTypesApi(application, mimeTypesFactory.getObject())
    }

    private ResponseMimeTypesApi getResponseMimeTypesAPi() {
        final application = new DefaultGrailsApplication()
        application.config = config
        def mimeTypesFactory = new MimeTypesFactoryBean()
        mimeTypesFactory.grailsApplication = application

        return new ResponseMimeTypesApi(application, mimeTypesFactory.getObject())
    }

    private getConfig() {
        def s = new ConfigSlurper()

        s.parse '''
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = true
grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
                      xml: ['text/xml', 'application/xml'],
                      text: 'text/plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      csv: 'text/csv',
                      all: '*/*',
                      json: ['application/json','text/json'],
                      form: 'application/x-www-form-urlencoded',
                      multipartForm: 'multipart/form-data'
                    ]
'''
    }
}
