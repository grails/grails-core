package grails.test.web

import grails.artefact.Artefact
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

/**
 * Tests that Groovy's @Mixin works on a controller
 */
class ControllerWithGroovyMixinSpec extends Specification implements ControllerUnitTest<MixedController> {

    void "Test that Groovy's mixin transform works with controllers"() {
        when:"An action with a mixin is executed"
            controller.index()

        then:"The mixin is used and available"
            response.text.contains "O HAI ITS"
    }
}

@Artefact("Controller")
@Mixin(Timestamps)
class MixedController {

    def index() {
        render contentType: "text/plain", text: "O HAI ITS $timestamp"
    }
}

class Timestamps {

    String getTimestamp() {
        new Date().format("HH:mm")
    }
}
