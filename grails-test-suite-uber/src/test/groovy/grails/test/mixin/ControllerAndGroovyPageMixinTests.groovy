package grails.test.mixin

import grails.artefact.Artefact
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class ControllerAndGroovyPageMixinTests extends Specification implements ControllerUnitTest<MyController> {

    // GRAILS-9718
    void testController() {
        expect:
        controller != null

        when:
        views['/foo/_bar.gsp'] = 'Id: ${params.id}'
        params.id = 10
        def content = render(template:"/foo/bar")

        then:
        content == 'Id: 10'
    }
}

@Artefact("Controller")
class MyController {
}
