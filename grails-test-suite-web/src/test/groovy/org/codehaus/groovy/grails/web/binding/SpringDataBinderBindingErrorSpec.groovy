package org.codehaus.groovy.grails.web.binding

import grails.artefact.Artefact
import grails.test.mixin.TestFor

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes

import spock.lang.Issue
import spock.lang.Specification


@TestFor(CommandController)
class SpringDataBinderBindingErrorSpec extends Specification {

    @Issue('GRAILS-11044')
    void 'Test binding errors when using the Spring binder'() {
        given: 'The spring binder is being used'
        grailsApplication.config.grails.databinding.useSpringBinder=true
        grailsApplication.configChanged()
        
        when: 'a binding error occurs'
        // this needs to be request.setParameter... params.number='forty two' does not trigger the relevant code
        request.setParameter 'number', 'forty two'
        controller.index()
        
        then: ' the non-domain class object has the expected binding error associated with it'
        response.contentAsString == 'Error Count: 1'
    }
}

@Artefact('Controller')
class CommandController {
    
    def index(House house) {
        render "Error Count: ${house.errors.errorCount}"
    }
}

class House {
    Integer number
}