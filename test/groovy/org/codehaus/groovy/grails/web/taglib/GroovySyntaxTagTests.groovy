/**
 * 
 */
package org.codehaus.groovy.grails.web.taglib;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.groovy.grails.web.pages.GroovyPage;

import junit.framework.TestCase;

/**
 * @author graemerocher
 *
 */
public class GroovySyntaxTagTests extends TestCase {

	private tag;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		tag = new MyGroovySyntaxTag();
	}


    protected void tearDown() {
        tag = null
    }

	/**
	 * Test method for {@link org.codehaus.groovy.grails.web.taglib.GroovySyntaxTag#init(java.util.Map)}.
	 */
	public void testInit() {
		Map ctx = new HashMap();
		ctx.put(GroovyPage.OUT, new PrintWriter(new StringWriter()));
		tag.init(ctx);
		assertEquals(tag.out,ctx.get(GroovyPage.OUT));
	}

	/**
	 * Test method for {@link org.codehaus.groovy.grails.web.taglib.GroovySyntaxTag#setAttributes(java.util.Map)}.
	 */
	public void testSetAttributes() {
		Map attrs = new HashMap();
		attrs.put("\"test1\"","value1");
		attrs.put("\"test2\"","value2");
		
		tag.setAttributes(attrs);
		
		assertNotNull(tag.attributes);
		assertFalse(tag.attributes.isEmpty());
		assertEquals(2, tag.attributes.size());
		assertTrue(tag.attributes.containsKey("test1"));
		assertTrue(tag.attributes.containsKey("test2"));
	}
	
	/**
	 * Test method for {@link org.codehaus.groovy.grails.web.taglib.GroovySyntaxTag#calculateExpression(java.lang.String)}.
	 */
	public void testCalculateExpression() {	
		assertEquals("test", tag.calculateExpression(" test "));
		assertEquals("test",tag.calculateExpression(" \" test\" "));
		assertEquals("test.method()", tag.calculateExpression(' ${ test.method() } ' ));
	}
}
class MyGroovySyntaxTag extends GroovySyntaxTag {
		public boolean hasPrecedingContent() {
			return false;
		}

		public boolean isBufferWhiteSpace() {
			return false;
		}

		public void doEndTag() {
			
		}

		public void doStartTag() {
			
		}

		public String getName() {
			return null;
		}	
}