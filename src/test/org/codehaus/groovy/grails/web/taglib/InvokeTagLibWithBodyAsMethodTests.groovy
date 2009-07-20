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