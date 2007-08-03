package org.codehaus.groovy.grails.web.taglib;

import org.springframework.validation.Errors;

class ValidationTagLibTests extends AbstractGrailsTagTests {

	void testHasErrorsWithRequestAttributes() {
		StringWriter sw = new StringWriter();

		withTag("hasErrors", sw) { tag ->

            def mockErrors = [:]
            
	        request.setAttribute("somethingErrors", mockErrors as Errors);

			// test when no message found it returns code
			def attrs = [:]
			tag.call( attrs, { "error found"} )

			assertEquals "error found", sw.toString()
		}
	}

}
