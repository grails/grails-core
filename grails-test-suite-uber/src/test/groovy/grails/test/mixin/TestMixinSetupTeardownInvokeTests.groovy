package grails.test.mixin

import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * @author Graeme Rocher
 */
@TestMixin(GrailsUnitTestMixin)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
        println "invoked 1"
        assert value == 'World!'
    }

    @Test
    void testThatSetupWasInvoked2() {
        println "invoked 2"
        assert System.getProperty(TestMixinSetupTeardownInvokeTests.name) == 'invoked'
    }
}
