@artifact.package@

import grails.test.*
import grails.test.mixin.*
import grails.test.mixin.domain.*
import grails.test.mixin.web.*
import org.junit.Test
import org.junit.After
import org.junit.Before

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestMixin(ControllerUnitTestMixin)
class @artifact.name@ {

    @artifact.testclass@ controller
    @Before
    void setUp() {
        controller = mockController(@artifact.testclass@)
    }

    @After
    void tearDown() {
        // Tear down logic here
    }

    @Test
    void testSomething() {

    }
}
