package grails.test.mixin

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Ignore
import spock.lang.Specification

@Ignore('grails-gsp is not on jakarta.servlet yet')
class TestForControllerWithoutMockDomainTests extends Specification implements ControllerUnitTest<ImpedimentsController> {

    void testEditImpediment() {
        def impedimentInstance = new Impediment(text:"blah")

        when:
        impedimentInstance.save()

        then:
        thrown(Exception)
    }
}

@Artefact("Controller")
class ImpedimentsController{}

@Entity
class Impediment {
    String id
    String text

    static mapping = {
        id generator: 'uuid'
    }
}
