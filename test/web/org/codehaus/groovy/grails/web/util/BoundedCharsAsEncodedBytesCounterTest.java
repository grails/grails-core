package org.codehaus.groovy.grails.web.util;

import junit.framework.TestCase;

public class BoundedCharsAsEncodedBytesCounterTest extends TestCase {

	public void testCalculation() throws Exception {
		BoundedCharsAsEncodedBytesCounter counter=new BoundedCharsAsEncodedBytesCounter(1024, "ISO-8859-1");
		counter.update("Hello öäåÖÄÅ!");
		assertEquals(13, counter.size());
		assertEquals(13, "Hello öäåÖÄÅ!".getBytes("ISO-8859-1").length);
		counter.update("Hello öäåÖÄÅ!");
		assertEquals(26, counter.size());
	}
}
