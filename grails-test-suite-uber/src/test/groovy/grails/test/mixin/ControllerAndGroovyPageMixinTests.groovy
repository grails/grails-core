package grails.test.mixin

import grails.artefact.Artefact
import grails.test.mixin.web.GroovyPageUnitTestMixin
import org.junit.Test

/**
 * @author Graeme Rocher
 */
@TestMixin(GroovyPageUnitTestMixin)
@TestFor(MyController)
class ControllerAndGroovyPageMixinTests {

    // GRAILS-9718
    @Test
    void testController() {
        controller != null

        views['/foo/_bar.gsp'] = 'Id: ${params.id}'

        params.id = 10
        def content = render(template:"/foo/bar")

        assert content == 'Id: 10'
    }
}

@Artefact("Controller")
class MyController {
}
