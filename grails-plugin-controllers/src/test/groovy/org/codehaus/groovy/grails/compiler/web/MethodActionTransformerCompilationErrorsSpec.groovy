package org.codehaus.groovy.grails.compiler.web

import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader
import org.springframework.web.context.request.RequestContextHolder

import spock.lang.FailsWith
import spock.lang.Specification

class MethodActionTransformerCompilationErrorsSpec extends Specification {

	static gcl

    void setupSpec() {
        gcl = new GrailsAwareClassLoader()
        def transformer = new MethodActionTransformer() {
                    @Override
                    boolean shouldInject(URL url) {
                        return true;
                    }
                }
        def transformer2 = new ControllerTransformer() {
                    @Override
                    boolean shouldInject(URL url) {
                        return true;
                    }
                }
        gcl.classInjectors = [transformer, transformer2]as ClassInjector[]
    }
	
	@FailsWith(CompilationFailedException.class)
	void "Test default parameter values"() {
		expect:
		    gcl.parseClass('''
		    class TestController {
		        def methodAction(int i = 42){}
		    }
		    ''')
	}

    def cleanupSpec() {
        RequestContextHolder.setRequestAttributes(null)
    }
}

