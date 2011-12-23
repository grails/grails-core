package grails.test.mixin

import spock.lang.Specification
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert


@TestMixin(GrailsUnitTestMixin)
class MetaClassCleanupSpec extends Specification {

    def "Test that meta classes are restored to prior state after test run"() {
        when:"A meta class is modified in the test"
            Author.metaClass.testMe = {-> "test"}
            Author.metaClass.testToo = {-> "second"}
            def a = new Author()
        then:"The methods are available"
            a.testMe() == "test"
            a.testToo() == "second"
    }

    @AfterClass
    static void checkCleanup() {
        def a = new Author()

        try {
            a.testMe()
            Assert.fail("Should have cleaned up meta class changes")
        } catch (MissingMethodException) {
        }
        
        try {
            a.testToo()
            Assert.fail("Should have cleaned up meta class changes")
        } catch (MissingMethodException) {
        }
    }

}
class Author {
    String name
}

