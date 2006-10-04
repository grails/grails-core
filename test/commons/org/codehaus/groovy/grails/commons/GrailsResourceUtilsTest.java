package org.codehaus.groovy.grails.commons;

import java.net.URL;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import junit.framework.TestCase;

public class GrailsResourceUtilsTest extends TestCase {

	private static final String TEST_URL = "file:///test/grails/app/grails-app/domain/Test.groovy";

	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testIsDomainClass() throws Exception {

        URL testUrl = new URL(GrailsResourceUtilsTest.TEST_URL);

		assertTrue(GrailsResourceUtils.isDomainClass(testUrl));
	}

	public void testGetClassNameResource() throws Exception {
		Resource r = new UrlResource(new URL(TEST_URL));
		
		assertEquals("Test", GrailsResourceUtils.getClassName(r));
	}

	public void testGetClassNameString() {
		assertEquals("Test", GrailsResourceUtils.getClassName(TEST_URL));
	}

	public void testIsGrailsPath() {
		assertTrue(GrailsResourceUtils.isGrailsPath(TEST_URL));
	}

}
