package grails.test.mixin

import grails.artefact.Artefact
import grails.persistence.Entity

import org.junit.Assert;
import org.junit.Test

@TestFor(ImpedimentsController)
class TestForControllerWithoutMockDomainTests {

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
