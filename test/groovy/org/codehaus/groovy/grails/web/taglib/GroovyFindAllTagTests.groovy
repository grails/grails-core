package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException;


/**
 * @author Jeff Brown
 *
 */
public class GroovyFindAllTagTests extends GroovyTestCase {

    def tag;
    def sw;

    protected void setUp() {
        super.setUp()
        sw = new StringWriter()
        tag = new GroovyFindAllTag()
        tag.setWriter(new PrintWriter(sw))
    }


    protected void tearDown() {
        tag = null
        sw = null
        super.tearDown()
    }

    void testIsBufferWhiteSpace() {
        assertFalse(tag.isBufferWhiteSpace())
    }

    void testHasPrecedingContent() {
        assertTrue(tag.hasPrecedingContent())
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
        assertEquals("myObj.findAll {it.age > 19}.each { ${System.properties['line.separator']}", sw.toString())
    }

    void testDoEndTag() {
        tag.doEndTag()
        assertEquals("}${System.properties['line.separator']}", sw.toString())
    }

    void testTagName() {
        assertEquals("findAll", tag.getName())
    }
}