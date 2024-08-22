package grails.test.mixin

import grails.artefact.Artefact
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Issue
import spock.lang.Specification

class TestForControllerWithNamePropertySpec extends Specification implements ControllerUnitTest<SomeController> {

    @Issue('grails/grails-core#10363')
    void "test referencing a controller with a 'name' property"() {
        when:
        controller

        then:
        notThrown ClassCastException
    }
}

@Artefact('Controller')
class SomeController {
    String name
}
