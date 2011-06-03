package org.codehaus.groovy.grails.web.pages

import org.codehaus.groovy.grails.web.taglib.AbstractGrailsTagTests

import junit.framework.TestCase

class ModifyOurScopeWithBodyTagTests extends AbstractGrailsTagTests {

    @Override
    protected void onSetUp() {
        gcl.parseClass '''
class OutScopeTagLib {
  def threeTimes = { attrs, body ->
    3.times {
        if (attrs.var)
            out << body((attrs.var):it)
        else
            out << body()
    }
  }
    def local = { attrs, body ->
        out << body(attrs.vars)
    }
    
    def ittest = { attrs, body ->
		out << body('hello')
    }
}
        '''
    }

    // test for GRAILS-5847
    void testModifyOuterScopeInTag() {

        // test with no body arguments
        def template = '<g:set var="counter" value="${1}"/><g:threeTimes>${counter++}</g:threeTimes>'

        assertOutputEquals '123', template

        // test with body arguments
        template = '<g:set var="counter" value="${1}"/><g:threeTimes var="x">${counter++}</g:threeTimes>'
        assertOutputEquals '123', template
    }

    // test for GRAILS-2675
    void testRestoreOuterVariableNamesWithBodyArguments() {
        def template = '<g:set var="counter" value="${9}"/><g:threeTimes var="counter">${counter++}</g:threeTimes>${counter}'
        assertOutputEquals '0129', template
		
		template = '<g:set var="counter" value="${1}"/><g:threeTimes var="counter">${counter}</g:threeTimes>${counter}'
		assertOutputEquals '0121', template
    }

    // test for GRAILS-7306
    void testRestoreOuterVariableNamesWithBodyArgumentsEvenIfOuterValueIsNull() {
        def template = '''<g:set var="foo" value="parentFooVal"/><g:set var="bar" value="${null}"/><g:local vars="[foo:'innerFooVal', bar:'nonNullVal']" someValue="nonNull" var="counter">inner foo: ${foo}, inner bar: ${bar}</g:local> outer foo: ${foo}, outer bar: ${bar}'''
        assertOutputEquals 'inner foo: innerFooVal, inner bar: nonNullVal outer foo: parentFooVal, outer bar: ', template
    }
	
	void testBodyIt() {
		def template = '''<g:set var="it" value=" world"/><g:ittest>${it}</g:ittest>${it}'''
		assertOutputEquals 'hello world', template
	}

}
