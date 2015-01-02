package org.grails.gsp.compiler.tags
import org.grails.gsp.GroovyPage
import org.grails.gsp.compiler.GroovyPageParser
import org.grails.taglib.GrailsTagException
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
