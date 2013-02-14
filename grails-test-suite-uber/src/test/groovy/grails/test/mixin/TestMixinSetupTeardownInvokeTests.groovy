package grails.test.mixin

import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test

/**
 * @author Graeme Rocher
 */
@TestMixin(GrailsUnitTestMixin)
class TestMixinSetupTeardownInvokeTests {

    def value
    void setUp() {
        value = 'World!'
    }

    void tearDown() {
        System.setProperty(TestMixinSetupTeardownInvokeTests.name, "invoked")
    }

    @Test
    void testThatSetupWasInvoked() {
        assert value == 'World!'
    }

    @Test
    void testThatSetupWasInvoked2() {
        assert System.getProperty(TestMixinSetupTeardownInvokeTests.name) == 'invoked'
    }
}
