package sharedruntimetest

import static org.junit.Assert.*;
import grails.test.mixin.TestMixin;
import grails.test.mixin.support.GrailsUnitTestMixin;
import grails.test.runtime.SharedRuntime;

import org.junit.Test

@SharedRuntime(MySharedRuntimeConfigurer)
@TestMixin(GrailsUnitTestMixin)
class SharedRuntimeSampleTest {
    @Test
    void testThatRuntimeHasBeenShared() {
        SharedRuntimeCheck.checkGrailsApplicationHasNotChanged(getGrailsApplication())
    }
}
