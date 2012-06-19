package grails.util

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class ClosureToMapPopulatorTests extends GroovyTestCase {

    void testPopulate() {
        def populator = new ClosureToMapPopulator()

        def result = populator.populate {
            foo = "bar"
            one "two"
            three "four", "five"
        }

        assertEquals "bar", result.foo
        assertEquals "two", result.one
        assertEquals "should have returned a list", ["four", "five"], result.three
    }
}
