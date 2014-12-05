package org.grails.gsp.compiler.tags

import junit.framework.Assert
import junit.framework.TestCase
import org.grails.gsp.GroovyPage

/**
 * @author graemerocher
 */
class GroovySyntaxTagTests extends TestCase {

    private tag = new MyGroovySyntaxTag()

    /**
     * Test method for {@link GroovySyntaxTag#init(java.util.Map)}.
     */
    void testInit() {
        Map ctx = [:]
        ctx.put(GroovyPage.OUT, new PrintWriter(new StringWriter()))
        tag.init(ctx)
        assertEquals(tag.out,ctx.get(GroovyPage.OUT))
    }

    /**
     * Test method for {@link GroovySyntaxTag#setAttributes(java.util.Map)}.
     */
    void testSetAttributes() {
        Map attrs = [:]
        attrs.put("\"test1\"","value1")
        attrs.put("\"test2\"","value2")

        tag.setAttributes(attrs)

        assertNotNull(tag.attributes)
        Assert.assertFalse(tag.attributes.isEmpty())
        TestCase.assertEquals(2, tag.attributes.size())
        Assert.assertTrue(tag.attributes.containsKey("test1"))
        Assert.assertTrue(tag.attributes.containsKey("test2"))
    }

    /**
     * Test method for {@link GroovySyntaxTag#calculateExpression(java.lang.String)}.
     */
    void testCalculateExpression() {
        TestCase.assertEquals("test", tag.calculateExpression(" test "))
        TestCase.assertEquals("test",tag.calculateExpression(" \" test\" "))
        TestCase.assertEquals("test.method()", tag.calculateExpression(' ${ test.method() } '))
    }
}

class MyGroovySyntaxTag extends GroovySyntaxTag {
    boolean isAllowPrecedingContent() { false }

    boolean isKeepPrecedingWhiteSpace() { false }

    void doEndTag() {}

    void doStartTag() {}

    String getName() { null }
}
