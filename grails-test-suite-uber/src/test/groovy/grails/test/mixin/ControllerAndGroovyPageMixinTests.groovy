package grails.test.mixin

import grails.test.mixin.web.GroovyPageUnitTestMixin
import org.junit.Test

/**
 * @author Graeme Rocher
 */
@TestMixin(GroovyPageUnitTestMixin)
@TestFor(MyController)
class ControllerAndGroovyPageMixinTests {

    // verifies the above 2 mixins can operator together without error
    @Test
    void testController() {
        controller != null
    }
}
