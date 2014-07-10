package org.codehaus.groovy.grails.web.controllers

import grails.artefact.Artefact
import grails.converters.JSON
import grails.converters.XML
import grails.test.mixin.TestFor
import org.grails.plugins.testing.GrailsMockHttpServletRequest
import org.grails.plugins.testing.GrailsMockHttpServletResponse

import spock.lang.Issue
import spock.lang.Specification

@TestFor(ContentNegotiationController)
class ContentNegotiationSpec extends Specification {
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
    
    void setup() {
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
        grailsApplication.config = config
    }
    
    @Issue("GRAILS-10897")
    void "test index json content negotiation"() {
        given:
            def title = "This controller title"
            controller.params.title = title
        when: 'the request is set to json'
            request.addHeader "Accept", "text/json"
            controller.index()
        then:
            'json' == controller.response.format
            controller.response.contentAsString == """{"title":"$title"}"""
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
