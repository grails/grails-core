package org.codehaus.groovy.grails.web.util;

import junit.framework.TestCase;

public class BoundedCharsAsEncodedBytesCounterTest extends TestCase {

    private static final String TEST_STRING = "Hello \u00f6\u00e4\u00e5\u00d6\u00c4\u00c5!";

    public void testCalculation() throws Exception {
        BoundedCharsAsEncodedBytesCounter counter = new BoundedCharsAsEncodedBytesCounter(1024, "ISO-8859-1");
        counter.update(TEST_STRING);
        assertEquals(13, counter.size());
        assertEquals(13, TEST_STRING.getBytes("ISO-8859-1").length);
        counter.update(TEST_STRING);
        assertEquals(26, counter.size());
    }
}
