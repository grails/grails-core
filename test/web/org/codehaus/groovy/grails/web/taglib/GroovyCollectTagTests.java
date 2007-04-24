/**
 * 
 */
package org.codehaus.groovy.grails.web.taglib;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException;

import junit.framework.TestCase;

/**
 * @author graemerocher
 *
 */
public class GroovyCollectTagTests extends TestCase {

	private GroovyCollectTag tag;
	private StringWriter sw;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		sw = new StringWriter();
		tag = new GroovyCollectTag();
		tag.setWriter(new PrintWriter(sw));
	}


    protected void tearDown() throws Exception
    {
        tag = null;
        sw = null;
        super.tearDown();
    }

    /**
	 * Test method for {@link org.codehaus.groovy.grails.web.taglib.GroovyCollectTag#isBufferWhiteSpace()}.
	 */
	public void testIsBufferWhiteSpace() {
		assertFalse(tag.isBufferWhiteSpace());
	}

	/**
	 * Test method for {@link org.codehaus.groovy.grails.web.taglib.GroovyCollectTag#hasPrecedingContent()}.
	 */
	public void testHasPrecedingContent() {
		assertTrue(tag.hasPrecedingContent());
	}

	/**
	 * Test method for {@link org.codehaus.groovy.grails.web.taglib.GroovyCollectTag#doStartTag()}.
	 */
	public void testDoStartTag() {
		Map attrs = new HashMap();
		
		try {
			tag.doStartTag();
			fail("can't create this tag with no [in] and [expr] attributes");
		} catch (GrailsTagException e) {
			// expected
		}
		attrs.put("\"in\"", "myObj");
		attrs.put("\"expr\"", " ${ it.name }");
		tag.setAttributes(attrs);
		assertFalse(tag.attributes.isEmpty());
		tag.doStartTag();		
		assertEquals("myObj.collect {it.name}.each { " + System.getProperty("line.separator"),sw.toString());
	}

}
