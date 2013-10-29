package org.codehaus.groovy.grails.web.taglib;

import org.codehaus.groovy.grails.web.pages.GroovyPage

import junit.framework.TestCase

/**
 * @author graemerocher
 */
class GroovySyntaxTagTests extends TestCase {

    private tag = new MyGroovySyntaxTag()

    /**
     * Test method for {@link org.codehaus.groovy.grails.web.taglib.GroovySyntaxTag#init(java.util.Map)}.
     */
    void testInit() {
        Map ctx = [:]
        ctx.put(GroovyPage.OUT, new PrintWriter(new StringWriter()))
        tag.init(ctx)
        assertEquals(tag.out,ctx.get(GroovyPage.OUT))
    }

    /**
     * Test method for {@link org.codehaus.groovy.grails.web.taglib.GroovySyntaxTag#setAttributes(java.util.Map)}.
     */
    void testSetAttributes() {
        Map attrs = [:]
        attrs.put("\"test1\"","value1")
        attrs.put("\"test2\"","value2")

        tag.setAttributes(attrs)

        assertNotNull(tag.attributes)
        assertFalse(tag.attributes.isEmpty())
        assertEquals(2, tag.attributes.size())
        assertTrue(tag.attributes.containsKey("test1"))
        assertTrue(tag.attributes.containsKey("test2"))
    }

    /**
     * Test method for {@link org.codehaus.groovy.grails.web.taglib.GroovySyntaxTag#calculateExpression(java.lang.String)}.
     */
    void testCalculateExpression() {
        assertEquals("test", tag.calculateExpression(" test "))
        assertEquals("test",tag.calculateExpression(" \" test\" "))
        assertEquals("test.method()", tag.calculateExpression(' ${ test.method() } '))
    }
}

class MyGroovySyntaxTag extends GroovySyntaxTag {
    boolean isAllowPrecedingContent() { false }

    boolean isKeepPrecedingWhiteSpace() { false }

    void doEndTag() {}

    void doStartTag() {}

    String getName() { null }
}
