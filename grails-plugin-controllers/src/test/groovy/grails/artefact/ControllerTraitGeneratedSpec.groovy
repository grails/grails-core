package grails.artefact

import groovy.transform.Generated
import spock.lang.Specification

import java.lang.reflect.Method

class ControllerTraitGeneratedSpec extends Specification {

    void "test that all Controller trait methods are marked as Generated"() {
        // It will test also RequestForwarder, ResponseRedirector and ResponseRenderer traits, cause Controller implements them

        expect: "all Controller methods are marked as Generated on implementation class"
        Controller.getMethods().each { Method traitMethod ->
            assert TestController.class.getMethod(traitMethod.name, traitMethod.parameterTypes).isAnnotationPresent(Generated)
        }
    }
}

class TestController implements Controller {

}
