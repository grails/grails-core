package org.codehaus.groovy.grails.web.controllers

import grails.artefact.Artefact
import grails.converters.JSON
import grails.converters.XML
import grails.test.mixin.TestFor
import spock.lang.Issue
import spock.lang.Specification

@TestFor(ContentNegotiationController)
class ContentNegotiationSpec extends Specification {
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
