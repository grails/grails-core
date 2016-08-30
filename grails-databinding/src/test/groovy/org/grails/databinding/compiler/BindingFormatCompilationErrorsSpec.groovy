package org.grails.databinding.compiler

import org.codehaus.groovy.control.MultipleCompilationErrorsException

import spock.lang.Issue
import spock.lang.Specification

class BindingFormatCompilationErrorsSpec extends Specification {

	@Issue('GRAILS-11321')
    void 'Test compiling @BindingFormat with no code and no value'() {
        given:
        def gcl = new GroovyClassLoader()
        
        when:
        gcl.parseClass '''
package com.demo

class SomeClass {
        @grails.databinding.BindingFormat
        String someProperty
}
'''
        then:
        MultipleCompilationErrorsException e = thrown()
        e.message.contains 'The @BindingFormat annotation on the field [someProperty] in class [com.demo.SomeClass] must provide a value for either the value() or code() attribute.'
    }

	void 'Test compiling @BindingFormat with code'() {
		given:
		def gcl = new GroovyClassLoader()
		
		when:
		def c = gcl.parseClass '''
package com.demo

class SomeClass {
		@grails.databinding.BindingFormat(code='foo')
		String someProperty
}
'''
		then:
		c
	}

	void 'Test compiling @BindingFormat with value'() {
		given:
		def gcl = new GroovyClassLoader()
		
		when:
		def c = gcl.parseClass '''
package com.demo

class SomeClass {
		@grails.databinding.BindingFormat('foo')
		String someProperty
}
'''
		then:
		c
	}
}
