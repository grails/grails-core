package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.commons.TagLibArtefactHandler

/**
 * @author Graeme Rocher
 * @since 0.4
 */
class PageScopeTests extends AbstractGrailsTagTests {

    void testNamedBodyParams() {
        def template = '<g:set var="foo" value="bar" />one: <g:test1 /> two: ${bar} three: ${pageScope.bar}'
        assertOutputEquals('one: bar two: foo three: foo', template)
    }

    void testScopes() {
        def template = '''
<g:set var="one" scope="request" value="two" />
<g:set var="two" scope="page" value="three" />
<g:set var="three" scope="session" value="four" />one: ${request.one} two: ${two} three: ${session.three}
'''

        assertOutputEquals('one: two two: three three: four', template, [:], { it.toString().trim() })
    }

    protected void onInit() {
        def tagClass = gcl.parseClass('''
class MyTagLib {
    def test1 = { attrs, body ->
        pageScope.bar = "foo"
        out << pageScope.foo
    }
}
''')
        grailsApplication.addArtefact(TagLibArtefactHandler.TYPE,tagClass)
    }
}
