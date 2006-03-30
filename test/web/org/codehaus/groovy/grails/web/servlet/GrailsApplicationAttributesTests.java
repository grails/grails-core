package org.codehaus.groovy.grails.web.servlet;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

import junit.framework.TestCase;

public class GrailsApplicationAttributesTests extends TestCase {

	/*
	 * Test method for 'org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes.getTemplateUri(String, ServletRequest)'
	 */
	public void testGetTemplateUri() {
		 GrailsApplicationAttributes attrs = new DefaultGrailsApplicationAttributes(new MockServletContext());
		 
		 assertEquals("/WEB-INF/grails-app/views/_test.gsp",attrs.getTemplateUri("/test", new MockHttpServletRequest()));
		 assertEquals("/WEB-INF/grails-app/views/shared/_test.gsp",attrs.getTemplateUri("/shared/test", new MockHttpServletRequest()));
	}

}
