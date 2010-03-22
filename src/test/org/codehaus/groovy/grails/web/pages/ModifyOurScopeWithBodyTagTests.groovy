package org.codehaus.groovy.grails.web.pages;

import org.codehaus.groovy.grails.web.taglib.AbstractGrailsTagTests;

import junit.framework.TestCase;

class ModifyOurScopeWithBodyTagTests extends AbstractGrailsTagTests {
	
	@Override
	public void onSetUp() {
		gcl.parseClass '''
class OutScopeTagLib {
  def threeTimes = { attrs, body ->
	3.times {
		if(attrs.var)
			out << body((attrs.var):it)
		else
			out << body()
	}
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
		def template = '<g:set var="counter" value="${1}"/><g:threeTimes var="counter">${counter++}</g:threeTimes>${counter}'
		assertOutputEquals '0121', template
	}
}
