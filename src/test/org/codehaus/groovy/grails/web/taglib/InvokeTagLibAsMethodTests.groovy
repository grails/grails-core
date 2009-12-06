package org.codehaus.groovy.grails.web.taglib

class InvokeTagLibAsMethodTests extends AbstractGrailsTagTests {

    void onSetUp() {
        gcl.parseClass('''
class TestTagLib {
    def testTypeConversion = { attrs ->
        out << "Number Is: ${attrs.int('number')}"
	}
}
''')
    }

    void testTypeConvertersWhenTagIsInvokedAsMethod() {
        // test for GRAILS-5484
        def template = '${g.testTypeConversion(number: "42")}'
        assertOutputEquals 'Number Is: 42', template
    }

}