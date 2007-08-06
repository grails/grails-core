package org.codehaus.groovy.grails.web.taglib;

import org.springframework.validation.Errors
import org.springframework.validation.FieldError;

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

	void testMessageTag() {
        def error = new FieldError("foo", "bar",1, false, ["my.error.code"] as String[], null, "This is default")
        def template = '<g:message error="${error}" />'

        assertOutputEquals("This is default", template, [error:error])
                
    }



}
