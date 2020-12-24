package grails.artefact.controller

import groovy.transform.Generated
import spock.lang.Specification

import java.lang.reflect.Method

class RestResponderTraitGeneratedSpec extends Specification {

    void "test that all RestResponder trait methods are marked as Generated"() {
        expect: "all RestResponder methods are marked as Generated on implementation class"
        RestResponder.getMethods().each { Method traitMethod ->
            assert TestResponder.class.getMethod(traitMethod.name, traitMethod.parameterTypes).isAnnotationPresent(Generated)
        }
    }
}

class TestResponder implements RestResponder {

}
