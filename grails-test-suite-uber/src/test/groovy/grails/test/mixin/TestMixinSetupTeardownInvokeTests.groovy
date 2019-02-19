package grails.test.mixin

import spock.lang.Specification
import spock.lang.Stepwise

/**
 * @author Graeme Rocher
 */
@Stepwise
class TestMixinSetupTeardownInvokeTests extends Specification {

    static int counter = 1

    def value

    void setup() {
        value = 'World!'
    }

    void cleanup() {
        System.setProperty(TestMixinSetupTeardownInvokeTests.name, "invoked")
    }

    void testThatSetupWasInvoked() {
        println "invoked 1 ${counter++} ${TestMixinSetupTeardownInvokeTests.class.hashCode()}"

        expect:
        value == 'World!'
    }

    void testThatSetupWasInvoked2() {
        println "invoked 2 ${counter++} ${TestMixinSetupTeardownInvokeTests.class.hashCode()}"

        expect:
        System.getProperty(TestMixinSetupTeardownInvokeTests.name) == 'invoked'
    }
}
