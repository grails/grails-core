package grails.test.mixin

import grails.artefact.Artefact
import spock.lang.Issue
import spock.lang.Specification

@TestFor(SomeController)
class TestForControllerWithNamePropertySpec extends Specification {

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
