package org.grails.web.pages

import org.grails.web.taglib.AbstractGrailsTagTests

/**
 * Tests rendering of static content.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class StaticContentRenderingTests extends AbstractGrailsTagTests {

    void testExpressionSpaces() {
        def template = '${x} ${y}'
        assertOutputEquals('1 2', template, [x:1, y:2])
    }

    void testExpressionVsScriptletOutput() {
        withConfig("grails.views.default.codec='HTML'") {
            def template = '${test}<%=test%>'
            assertOutputEquals('&lt;html&gt;&lt;body&gt;hello&lt;/body&gt;&lt;/html&gt;<html><body>hello</body></html>', template, [test:"<html><body>hello</body></html>"])
        }
    }

    void testImports() {
        def template = '<%@page import="java.text.SimpleDateFormat"%><% format = new SimpleDateFormat() %>${format.getClass()}'
        assertOutputEquals('class java.text.SimpleDateFormat', template)
    }

    void testHtmlEscaping() {
        withConfig("grails.views.default.codec='HTML'") {
            def template = '${test}'
            assertOutputEquals('&lt;html&gt;&lt;body&gt;hello&lt;/body&gt;&lt;/html&gt;', template, [test:"<html><body>hello</body></html>"])
        }
    }

    void testHtmlEscapingLowerCase() {
        withConfig("grails.views.default.codec='html'") {
            def template = '${test}'
            assertOutputEquals('&lt;html&gt;&lt;body&gt;hello&lt;/body&gt;&lt;/html&gt;', template, [test:"<html><body>hello</body></html>"])
        }
    }

    void testHtmlEscapingWithPageDirective() {
        def template = '<%@page defaultCodec="HTML" %>${test}'
        assertOutputEquals('&lt;html&gt;&lt;body&gt;hello&lt;/body&gt;&lt;/html&gt;', template, [test:"<html><body>hello</body></html>"])
    }

    void testNotHtmlEscaping() {
        def template = '<%@ contentType="text/plain" defaultCodec="none" %>${test}'
        assertOutputEquals('<html><body>hello</body></html>', template, [test:"<html><body>hello</body></html>"])
    }

    void testDisabledHtmlEscaping() {
        withConfig("grails.views.default.codec='none'") {
            def template = '${test}'
            assertOutputEquals('<html><body>hello</body></html>', template, [test: "<html><body>hello</body></html>"])
        }
    }

    void testStaticContent() {
        def template = '<div><g:each in="${numbers}"><p>${it}</p></g:each></div>'

        assertOutputEquals('<div><p>1</p><p>2</p><p>3</p></div>', template, [numbers:[1,2,3]])
    }

    void testGspComments() {
        def template = '''<div><%--
<g:each in="${numbers}">
    <p>${it}</p>
</g:each>--%>
</div>'''

        assertOutputEquals('<div>\n</div>', template, [numbers:[1,2,3]])
    }
    
    void testNamespacedXmlNoBody() {
        // GRAILS-10525
        def template = '''<esi:include src="foo.html"/>'''
        assertOutputEquals('<esi:include src="foo.html"/>', template, [:])
    }

    void testNamespacedXmlWithBody() {
        def template = '''<xhtml:p>body</xhtml:p>'''
        assertOutputEquals('<xhtml:p>body</xhtml:p>', template, [:])
    }

}
