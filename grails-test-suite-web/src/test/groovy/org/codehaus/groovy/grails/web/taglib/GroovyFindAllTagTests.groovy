package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException

/**
 * @author Jeff Brown
 */
class GroovyFindAllTagTests extends GroovyTestCase {

    def tag = new GroovyFindAllTag()
    def sw = new StringWriter()

    protected void setUp() {
        super.setUp()
        tag.setWriter(new PrintWriter(sw))
    }

    void testIsBufferWhiteSpace() {
        assertFalse(tag.isKeepPrecedingWhiteSpace())
    }

    void testHasPrecedingContent() {
        assertTrue(tag.isAllowPrecedingContent())
    }

    void testDoStartWithNoInAttribute() {
        tag.attributes = ['"expr"': " someExpression "]
        shouldFail(GrailsTagException) {
            tag.doStartTag()
        }
    }

    void testDoStartWithNoExprAttribute() {
        tag.attributes = ['"in"': " someExpression "]
        shouldFail(GrailsTagException) {
            tag.doStartTag()
        }
    }

    void testDoStartTag() {
        tag.attributes = ['"expr"': " \${it.age > 19}", '"in"': "myObj"]
        tag.doStartTag()

        assertEquals("for( it in myObj.findAll {it.age > 19} ) {"+System.getProperty("line.separator"), sw.toString())
    }

    void testDoEndTag() {
        tag.doEndTag()
        assertEquals("}${System.properties['line.separator']}", sw.toString())
    }

    void testTagName() {
        assertEquals("findAll", tag.getName())
    }
}
