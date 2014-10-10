package grails.artefact

import grails.artefact.controller.RestResponder
import grails.artefact.controller.support.ResponseRenderer
import grails.web.api.WebAttributes
import grails.web.databinding.DataBinder
import spock.lang.Specification

class ControllerTraitSpec extends Specification {

    void 'test that a class marked with @Artefact("Controller") is enhanced with expected traits'() {
        expect:
        Controller.isAssignableFrom SomeController
        WebAttributes.isAssignableFrom SomeController
        ResponseRenderer.isAssignableFrom SomeController
        DataBinder.isAssignableFrom SomeController
        RestResponder.isAssignableFrom SomeController
    }
}

@Artefact('Controller')
class SomeController {
}
