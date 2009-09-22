package org.codehaus.groovy.grails.web.taglib
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jun 25, 2009
 */

public class InvokeTagLibWithBodyAsMethodTests extends AbstractGrailsTagTests {

    public void onSetUp() {
        gcl.parseClass('''
class TestTagLib {
    def testInvokeWithBodyClosure = { attrs, body ->
        out << eachItem(items:[1,2,3]) { bodyAttrs ->
             out << "body=${bodyAttrs.var}"
        }
    }
    def eachItem = { attrs, body ->
        def items = attrs.items
        items.each { i ->
            out << body(var:i)
        }
    }
    def testWithClosureAndGStringReturn = { attrs, body ->
        def foo = "bar"
		out << "one" << test(foo:"bar") { "$foo" } << "four"
	}

    def testWithClosureAndStringReturn = { attrs, body ->
        
		out << "one" << test(foo:"bar") { "foo" } << "four"
	}

    def testWithGStringBody = { attrs, body ->
        def foo = "bar"
		out << "one" << test(foo:"bar", "$foo") << "four"
	}

	def testWithStringBody = { attrs, body ->
		out << "one" << test(foo:"bar", "foo") << "four"
	}

    def testWithResultOfBody= { attrs, body ->
		out << "one" << test(foo:"bar", body()) << "four"
	}

    def testWithClosureBody = { attrs, body ->
        out << "one" << test(foo:"bar") {
            out << "big" << "body" 
        }
        out << "four"
    }
	def test = { attrs, body ->
		def value = body()       
		out << "two" << value << "three"
	}
}
''')
    }


    void testWithResultOfBody() {
        def template = '<g:testWithResultOfBody>foo</g:testWithResultOfBody>'


        assertOutputEquals 'onetwofoothreefour', template

    }
    void testInvokeWithBodyClosure() {
        def template = '<g:testInvokeWithBodyClosure />'


        assertOutputEquals 'body=1body=2body=3', template

    }
    void testWithClosureAndGStringReturn() {
        def template = '<g:testWithClosureAndGStringReturn />'


        assertOutputEquals 'onetwobarthreefour', template
    }


    void testWithClosureAndStringReturn() {
        def template = '<g:testWithClosureAndStringReturn />'


        assertOutputEquals 'onetwofoothreefour', template
    }
    
    void testInvokeTagLibAsMethodWithGString() {
        def template = '<g:testWithGStringBody />'


        assertOutputEquals 'onetwobarthreefour', template
    }


    void testInvokeTagLibAsMethodWithString() {
        def template = '<g:testWithStringBody />'


        assertOutputEquals 'onetwofoothreefour', template
    }

    void testInvokeTagLibAsMethodWithClosure() {
        def template = '<g:testWithClosureBody />'


        assertOutputEquals 'onetwobigbodythreefour', template
    }

}