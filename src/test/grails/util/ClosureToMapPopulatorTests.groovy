package grails.util
/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class ClosureToMapPopulatorTests extends GroovyTestCase{

    void testPopulate() {
        def populator = new ClosureToMapPopulator()

        def result = populator.populate {
            foo = "bar"
            one "two"
            three "four", "five"
        }


        assertEquals "bar", result.foo
        assertEquals "two", result.one
        assert ["four", "five"] == result.three : "should have returned a list"
    }
}