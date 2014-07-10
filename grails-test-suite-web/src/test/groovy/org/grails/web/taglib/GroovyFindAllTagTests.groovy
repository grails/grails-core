package org.grails.web.taglib

import org.grails.web.taglib.GroovyFindAllTag

import java.io.ByteArrayInputStream
import java.io.PrintWriter
import java.util.HashMap
import java.util.Map

import org.grails.web.pages.GroovyPage
import org.grails.web.pages.GroovyPageParser
import org.grails.web.taglib.exceptions.GrailsTagException

/**
 * @author Jeff Brown
 */
class GroovyFindAllTagTests extends GroovyTestCase {

    def tag = new GroovyFindAllTag()
    def sw = new StringWriter()

    protected void setUp() {
        super.setUp()
        Map context = new HashMap();
        context.put(GroovyPage.OUT, new PrintWriter(sw));
        GroovyPageParser parser=new GroovyPageParser("test", "test", "test", new ByteArrayInputStream([] as byte[]));
        context.put(GroovyPageParser.class, parser);
        tag.init(context);
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

        assertEquals("for( "+tag.getForeachRenamedIt()+" in evaluate('myObj.findAll {it.age > 19}', 1, it) { return myObj.findAll {it.age > 19} } ) {"+System.getProperty("line.separator")+ "changeItVariable(" + tag.getForeachRenamedIt() + ")" + System.getProperty("line.separator"), sw.toString())
    }

    void testDoEndTag() {
        tag.doEndTag()
        assertEquals("}${System.properties['line.separator']}", sw.toString())
    }

    void testTagName() {
        assertEquals("findAll", tag.getName())
    }
}
