package org.codehaus.groovy.grails.web.mime

import grails.artefact.Artefact
import grails.test.mixin.TestFor
import spock.lang.Issue
import spock.lang.Specification

@TestFor(FormatController)
class WithFormatContentTypeSpec extends Specification {

    void setupSpec() {
        // unit tests in real applications will not need to do 
        // this because the real Config.groovy will be loaded
        grailsApplication.config.grails.mime.types = [(MimeType.ALL.extension): MimeType.ALL.name,
                                                      (MimeType.FORM.extension): MimeType.FORM.name,
                                                      (MimeType.JSON.extension): MimeType.JSON.name]
    }
    
    @Issue('GRAILS-11093')
    void 'Test specifying contentType'() {
        when: 'content type is specified'
        request.contentType = MimeType.FORM.name
        controller.index()
        
        then: 'the corresponding block is executed'
        response.status == 200
        view == '/formView'
    }

    @Issue('GRAILS-11093')
    void 'Test not specifying contentType'() {
        when: 'no content type is specified'
        controller.index()
        
        then: 'the wildcard block is executed'
        response.status == 200
        view == '/wildcardView'
    }

    @Issue('GRAILS-11093')
    void 'Test specifying request format'() {
        when: 'a request format is specified'
        request.format = 'form'
        controller.index()
        
        then: 'the corresponding block is executed'
        response.status == 200
        view == '/formView'
    }
}

@Artefact('Controller')
class FormatController {
    
    def index() {
        request.withFormat {
            form {
                render view: '/formView'
            }
            '*' {
                render view: '/wildcardView'
            }
        }
    }
}
