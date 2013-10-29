package grails.test.mixin

import grails.test.GrailsUnitTestCase

/**
 * Test for GRAILS-8290
 */
class RegisterMetaClassTests extends GrailsUnitTestCase {
    void setUp() {
        super.setUp()
        Book.metaClass.'static'.beforeRegister = { -> "beforeRegister exists" }
        registerMetaClass(Book)
        Book.metaClass.'static'.afterRegister = {-> "afterRegister exists" }
    }

    void tearDown() {
        super.tearDown()
        assert "beforeRegister exists" == Book.beforeRegister()
        shouldFail(MissingMethodException) {
            assert "afterRegister exists" == Book.afterRegister()
        }

    }

    void testSomething() {
        assert "beforeRegister exists" == Book.beforeRegister()
        assert "afterRegister exists" == Book.afterRegister()
    }
}
class Book {
    String title
}