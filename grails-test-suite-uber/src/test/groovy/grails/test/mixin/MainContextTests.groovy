package grails.test.mixin

import org.grails.testing.GrailsUnitTest
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class MainContextTests extends Specification implements GrailsUnitTest {

    void setup() {
        defineBeans {
            myService(MyService)
        }
        //uncomment to make test works
        //grailsApplication.mainContext = applicationContext
    }

    void testGetBean() {
        expect:
        grailsApplication.mainContext.getBean('myService')
    }
}
