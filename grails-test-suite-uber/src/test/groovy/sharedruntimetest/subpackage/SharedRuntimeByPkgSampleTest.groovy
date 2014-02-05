package sharedruntimetest.subpackage

import static org.junit.Assert.*
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin

import org.junit.Test

import sharedruntimetest.SharedRuntimeCheck

@TestMixin(GrailsUnitTestMixin)
class SharedRuntimeByPkgSampleTest {
    @Test
    void testThatRuntimeHasBeenShared() {
        SharedRuntimeCheck.checkGrailsApplicationHasNotChanged(getGrailsApplication())
    }
}
