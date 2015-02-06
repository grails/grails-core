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
    static int counter=1

    def value
    void setUp() {
        value = 'World!'
    }

    void tearDown() {
        System.setProperty(TestMixinSetupTeardownInvokeTests.name, "invoked")
    }

    @Test
    void testThatSetupWasInvoked() {
        println "invoked 1 ${counter++} ${TestMixinSetupTeardownInvokeTests.class.hashCode()}"
        assert value == 'World!'
    }

    @Test
    void testThatSetupWasInvoked2() {
        println "invoked 2 ${counter++} ${TestMixinSetupTeardownInvokeTests.class.hashCode()}"
        assert System.getProperty(TestMixinSetupTeardownInvokeTests.name) == 'invoked'
    }
}
