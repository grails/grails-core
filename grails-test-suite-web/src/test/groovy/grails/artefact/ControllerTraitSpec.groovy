package grails.artefact

import spock.lang.Specification

class ControllerTraitSpec extends Specification {

    void 'test that a class marked with @Artefact("Controller") is enhanced with grails.artefact.Controller'() {
        expect:
        Controller.isAssignableFrom SomeController
    }
}

@Artefact('Controller')
class SomeController {
}
