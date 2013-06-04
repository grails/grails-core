@artifact.package@import grails.test.AbstractCliTestCase

class @artifact.name@ extends AbstractCliTestCase {
    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void test@gant.class.name@() {

        execute(["@gant.script.name@"])

        assertEquals 0, waitForProcess()
        verifyHeader()
    }
}
