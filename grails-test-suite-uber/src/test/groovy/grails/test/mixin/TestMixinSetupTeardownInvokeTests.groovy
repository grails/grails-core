package grails.test.mixin

import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test

/**
 * @author Graeme Rocher
 */
@TestMixin(GrailsUnitTestMixin)
class TestMixinSetupTeardownInvokeTests {

    def value
    int counter
    void setUp() {
        value = 'World!'
    }

    void tearDown() {
        counter++
    }

    @Test
    void testThatSetupWasInvoked() {
        value == 'World!'
    }

    @Test
    void testThatSetupWasInvoked2() {
        counter == 1
    }
}
