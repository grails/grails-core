package grails.test.mixin

import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test

/**
 * @author Graeme Rocher
 */
@TestMixin(GrailsUnitTestMixin)
class MainContextTests {
    void setUp() {
        defineBeans {
            myService(MyService)
        }
        //uncomment to make test works
        //grailsApplication.mainContext = applicationContext
    }

    @Test
    void testGetBean() {
        assert grailsApplication.mainContext.getBean('myService')
    }
}
