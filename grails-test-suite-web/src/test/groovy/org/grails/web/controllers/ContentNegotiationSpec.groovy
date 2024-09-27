package org.grails.web.controllers

import grails.artefact.Artefact
import grails.converters.JSON
import grails.converters.XML
import grails.testing.web.controllers.ControllerUnitTest
import org.grails.plugins.testing.GrailsMockHttpServletRequest
import org.grails.plugins.testing.GrailsMockHttpServletResponse
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

class ContentNegotiationSpec extends Specification implements ControllerUnitTest<ContentNegotiationController> {

    Closure doWithConfig() {{ config ->
        config.grails.mime.use.accept.header=true
        config.grails.mime.types = [ // the first one is the default format
                                     html:          ['text/html','application/xhtml+xml'],
                                     all:           '*/*',
                                     atom:          'application/atom+xml',
                                     css:           'text/css',
                                     csv:           'text/csv',
                                     form:          'application/x-www-form-urlencoded',
                                     js:            'text/javascript',
                                     json:          ['application/json', 'text/json'],
                                     multipartForm: 'multipart/form-data',
                                     rss:           'application/rss+xml',
                                     text:          'text/plain',
                                     hal:           ['application/hal+json','application/hal+xml'],
                                     xml:           ['text/xml', 'application/xml']
        ]
    }}

    void setupSpec() {
        removeAllMetaClasses(GrailsMockHttpServletRequest)
        removeAllMetaClasses(GrailsMockHttpServletResponse)
    }
    
    void removeAllMetaClasses(Class clazz) {
        GroovySystem.metaClassRegistry.removeMetaClass clazz
        if(!clazz.isInterface()) {
            def superClazz = clazz.getSuperclass()
            if(superClazz) {
                removeAllMetaClasses(superClazz)
            }
        }
        for(Class interfaceClazz : clazz.getInterfaces()) {
            removeAllMetaClasses(interfaceClazz)
        }
    }
    
    @Unroll
    @Issue(["GRAILS-10897", "GRAILS-11954"])
    void "test render as #converter returns response with content-type '#contentType'"() {
        given:
            def title = "This controller title"
            controller.params.title = title

        when: 'the request is set to json'
            request.addHeader "Accept", acceptType
            controller.index()

        then:
            controller.response.format == responseFormat
            controller.response.contentAsString == contentAsString
            controller.response.contentType.tokenize(/;/)[0] == contentType

        where:
            acceptType         || responseFormat || contentType        || contentAsString
            "text/json"        || 'json'         || "application/json" || /{"title":"This controller title"}/  //defaults to application/json
            "application/json" || 'json'         || "application/json" || /{"title":"This controller title"}/
            "application/xml"  || 'xml'          || "application/xml"  || '''<?xml version="1.0" encoding="UTF-8"?><map><entry key="title">This controller title</entry></map>'''

            converter = responseFormat.toUpperCase()
    }
}

@Artefact("Controller")
class ContentNegotiationController {
    def index() {
        withFormat {
            xml { render params as XML }
            json { render params as JSON }
        }
    }
}
