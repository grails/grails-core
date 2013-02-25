package grails.test.mixin

import grails.artefact.Artefact
import grails.persistence.Entity

import org.junit.Test

@TestFor(ImpedimentsController)
class TestForControllerWithoutMockDomainTests {

    @Test
    void testEditImpediment() {
        def impedimentInstance = new Impediment(text:"blah")

        try {
            impedimentInstance.save()
        }
        catch(MissingMethodException me) {
            assert me.message.contains( "No signature of method: grails.test.mixin.Impediment.save() is applicable for argument types: () values: []" )
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
