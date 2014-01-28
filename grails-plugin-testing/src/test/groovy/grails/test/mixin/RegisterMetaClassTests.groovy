package grails.test.mixin

import grails.test.GrailsUnitTestCase

/**
 * Test for GRAILS-8290
 */
class RegisterMetaClassTests extends GrailsUnitTestCase {
    void setUp() {
        super.setUp()
        MCBook.metaClass.'static'.beforeRegister = { -> "beforeRegister exists" }
        registerMetaClass(MCBook)
        MCBook.metaClass.'static'.afterRegister = {-> "afterRegister exists" }
    }

    void tearDown() {
        super.tearDown()
        assert "beforeRegister exists" == MCBook.beforeRegister()
        shouldFail(MissingMethodException) {
            assert "afterRegister exists" == MCBook.afterRegister()
        }

    }

    void testSomething() {
        assert "beforeRegister exists" == MCBook.beforeRegister()
        assert "afterRegister exists" == MCBook.afterRegister()
    }
}
class MCBook {
    String title
}