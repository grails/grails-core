package grails.test.mixin

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.testing.web.controllers.ControllerUnitTest
import org.junit.Assert;
import org.junit.Test

class TestForControllerWithoutMockDomainTests implements ControllerUnitTest<ImpedimentsController> {

    @Test
    void testEditImpediment() {
        def impedimentInstance = new Impediment(text:"blah")

        try {
            impedimentInstance.save()
            Assert.fail("Exception should have been thrown")
        }
        catch(Exception e) {
            
        }
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
