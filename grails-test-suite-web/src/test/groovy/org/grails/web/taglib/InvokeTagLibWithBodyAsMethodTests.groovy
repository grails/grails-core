package org.grails.web.taglib
/**
 * @author Graeme Rocher
 * @since 1.0
 */
class InvokeTagLibWithBodyAsMethodTests extends AbstractGrailsTagTests {

    protected void onSetUp() {
        gcl.parseClass('''
import grails.gsp.*

@TagLib
class TestTagLib {
    Closure testInvokeWithBodyClosure = { attrs, body ->
        out << eachItem(items:[1,2,3]) { bodyAttrs ->
             out << "body=${bodyAttrs.var}"
        }
    }
    Closure eachItem = { attrs, body ->
        def items = attrs.items
        items.each { i ->
            out << body(var:i)
        }
    }
    Closure testWithClosureAndGStringReturn = { attrs, body ->
        def foo = "bar"
        out << "one" << test(foo:"bar") { "$foo" } << "four"
    }

    Closure testWithClosureAndStringReturn = { attrs, body ->

        out << "one" << test(foo:"bar") { "foo" } << "four"
    }

    Closure testWithGStringBody = { attrs, body ->
        def foo = "bar"
        out << "one" << test(foo:"bar", "$foo") << "four"
    }

    Closure testWithStringBody = { attrs, body ->
        out << "one" << test(foo:"bar", "foo") << "four"
    }

    Closure testWithResultOfBody= { attrs, body ->
        out << "one" << test(foo:"bar", body()) << "four"
    }

    Closure testWithClosureBody = { attrs, body ->
        out << "one" << test(foo:"bar") {
            out << "big" << "body"
        }
        out << "four"
    }
    Closure test = { attrs, body ->
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
