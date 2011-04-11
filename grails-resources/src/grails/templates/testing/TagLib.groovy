@artifact.package@
import grails.test.*
import grails.test.mixin.*
import grails.test.mixin.domain.*
import grails.test.mixin.web.*
import org.junit.*
import org.junit.After
import org.junit.Before

/**
 * See the API for {@link grails.test.mixin.web.GroovyPageUnitTestMixin} for usage instructions
 */
@TestMixin(GroovyPageUnitTestMixin)
class @artifact.name@ {

    @Before
    void setUp() {
        mockTagLib(@artifact.testclass@)
    }

    @After
    void tearDown() {
        // Tear down logic here
    }

    @Test
    void testSomething() {

    }
}
