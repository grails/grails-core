package org.codehaus.groovy.grails.web.taglib;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException;

/**
 * @author graemerocher
 */
public class GroovyCollectTagTests extends TestCase {

    private GroovyCollectTag tag = new GroovyCollectTag();
    private StringWriter sw = new StringWriter();

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tag.setWriter(new PrintWriter(sw));
    }

    /**
     * Test method for {@link org.codehaus.groovy.grails.web.taglib.GroovyCollectTag#isKeepPrecedingWhiteSpace()}.
     */
    public void testIsKeepPrecedingWhiteSpace() {
        assertTrue(tag.isKeepPrecedingWhiteSpace());
    }

    /**
     * Test method for {@link org.codehaus.groovy.grails.web.taglib.GroovyCollectTag#isAllowPrecedingContent()}.
     */
    public void testIsAllowPrecedingContent() {
        assertTrue(tag.isAllowPrecedingContent());
    }

    /**
     * Test method for {@link org.codehaus.groovy.grails.web.taglib.GroovyCollectTag#doStartTag()}.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testDoStartTag() {
        Map attrs = new HashMap();

        try {
            tag.doStartTag();
            fail("can't create this tag with no [in] and [expr] attributes");
        }
        catch (GrailsTagException e) {
            // expected
        }
        attrs.put("\"in\"", "myObj");
        attrs.put("\"expr\"", " ${ it.name }");
        tag.setAttributes(attrs);
        assertFalse(tag.attributes.isEmpty());
        tag.doStartTag();
        assertEquals("for( it in myObj.collect {it.name} ) {"+ System.getProperty("line.separator"),sw.toString());
    }
}
