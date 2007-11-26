/**
 * Tests rendering of static content
 
 * @author Graeme Rocher
 * @since 1.0
  *
 * Created: Sep 11, 2007
 * Time: 10:41:58 PM
 * 
 */
package org.codehaus.groovy.grails.web.pages

import org.codehaus.groovy.grails.web.taglib.AbstractGrailsTagTests
import org.codehaus.groovy.grails.commons.ConfigurationHolder

class StaticContentRenderingTests extends AbstractGrailsTagTests {


    void testDefaultHtmlEscaping() {
        def template = '${test}'

        assertOutputEquals('&lt;html&gt;&lt;body&gt;hello&lt;/body&gt;&lt;/html&gt;', template, [test:"<html><body>hello</body></html>"])
    }

    void testNotHtmlEscaping() {
        def template = '<%@ contentType="text/plain" %> ${test}'

        assertOutputEquals('<html><body>hello</body></html>', template, [test:"<html><body>hello</body></html>"])

    }

    void testDisabledHtmlEscaping() {
        def config = new ConfigSlurper().parse('grails.views.autoEscapeHtml=false')
        try {
            ConfigurationHolder.config = config
            def template = '${test}'
            assertOutputEquals('<html><body>hello</body></html>', template, [test: "<html><body>hello</body></html>"])
        } finally {
            ConfigurationHolder.config = null
        }



        
    }

    
    void testStaticContent() {


        def template = '''<div>
  <g:each in="${numbers}">
    <p>${it}</p>
  </g:each>
</div>'''

        assertOutputEquals('<div>    <p>1</p>    <p>2</p>    <p>3</p></div>', template, [numbers:[1,2,3]])
    }

    void testGspComments() {
        def template = '''<div><%--
<g:each in="${numbers}">
    <p>${it}</p>
</g:each>--%>
</div>'''

        assertOutputEquals('<div>\n</div>', template, [numbers:[1,2,3]])
    }
}