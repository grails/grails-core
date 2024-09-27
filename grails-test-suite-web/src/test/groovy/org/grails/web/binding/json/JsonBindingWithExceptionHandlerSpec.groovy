package org.grails.web.binding.json

import grails.artefact.Artefact
import grails.testing.web.controllers.ControllerUnitTest
import org.grails.databinding.bindingsource.DataBindingSourceCreationException
import spock.lang.Specification

class JsonBindingWithExceptionHandlerSpec extends Specification implements ControllerUnitTest<BindingWithExceptionHandlerMethodController> {

    void 'test binding malformed JSON'() {
        given:
        request.contentType = JSON_CONTENT_TYPE
        request.method = 'POST'
        request.JSON = '{"mapData": {"name":"Jeff{{{"'

        when:
        controller.bindWithCommandObject()
        
        then:
        response.status == 400
        model.errorMessage == 'caught a DataBindingSourceCreationException'
        view == '/bindingProblems'
    }

    void 'test binding valid JSON'() {
        given:
        request.contentType = JSON_CONTENT_TYPE
        request.method = 'POST'
        request.JSON = '{"name":"Jeff"}'

        when:
        def model = controller.bindWithCommandObject()
        
        then:
        response.status == 200
        flash.message == null
        model.command.name == 'Jeff'
        
    }
}

@Artefact('Controller')
class BindingWithExceptionHandlerMethodController {
    
    def bindWithCommandObject(SomeCommandObject co) {
        [command: co]
    }
    
    def handleDataBindingException(DataBindingSourceCreationException e) {
        response.status = 400
        render view: '/bindingProblems', model: [errorMessage: 'caught a DataBindingSourceCreationException']
    }
} 

class SomeCommandObject {
    String name
    static constraints = {
        name matches: /[A-Z].*/
    }
}
