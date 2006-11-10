package org.codehaus.groovy.grails.web.taglib;

import org.codehaus.groovy.grails.web.pages.*;

class GroovyEachParseTests extends ParseTests {

	void testEachOutput() {
		String output = parseCode("myTest", """
<g:each var="t" in="${'blah'}">
</g:each>
""");	
		def expected = '"blah".each { t ->'

		assert output.indexOf(expected)
	}

}
