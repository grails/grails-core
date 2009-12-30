package org.codehaus.groovy.grails.compiler

import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoader
import org.springframework.core.io.Resource
import org.springframework.core.io.ByteArrayResource
import org.codehaus.groovy.control.MultipleCompilationErrorsException

/**
 * @author Graeme Rocher
 * @since 1.2
 */

public class GrailsClassLoaderTests extends GroovyTestCase{


    void testCompilationErrorCapturing() {

        def gcl = new MockGrailsClassLoader()

        // test compilation errors
        gcl.groovySources["Bar"] = "class Bar {"

        assert gcl.parseClass("class Bar {}") : "should have parsed a class"

        shouldFail(MultipleCompilationErrorsException) {
            gcl.reloadClass("Bar")
        }

        try {
            gcl.reloadClass("Bar")
        }
        catch (e) {
            assert e == gcl.getCompilationError() : "should have stored compilation error"
        }


        gcl.groovySources["Bar"] = "class Bar {}"


        assert gcl.reloadClass("Bar") : "should have reloaded  class"
        assert !gcl.getCompilationError() : "shouldn't have any compilation errors" 
    }
}
class MockGrailsClassLoader extends GrailsClassLoader {

    Map groovySources = [:]
    
    protected Resource loadGroovySource(String name) {
        if(groovySources[name]) {
            return new ByteArrayResource(groovySources[name].bytes)
        }
    }


}