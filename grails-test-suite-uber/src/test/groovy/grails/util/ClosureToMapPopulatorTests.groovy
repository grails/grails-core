package grails.util

import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class ClosureToMapPopulatorTests {

    @Test
    void testPopulate() {
        def populator = new ClosureToMapPopulator()

        def result = populator.populate {
            foo = "bar"
            one "two"
            three "four", "five"
        }

        assertEquals "bar", result.foo
        assertEquals "two", result.one
        assertEquals(["four", "five"], result.three, "should have returned a list")
    }
}
