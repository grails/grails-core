package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.commons.TagLibArtefactHandler

/**
 * @author Graeme Rocher
 * @since 0.4
 */
class NamedTagBodyParamsTests extends AbstractGrailsTagTests {

    void testNamedBodyParams() {
        def template = '<g:test1>foo: ${foo} one: ${one}</g:test1>'
        assertOutputEquals('foo: bar one: 2', template)
    }

    void testOverridingParam() {
        def template = '''\
<g:set var="foo">Test</g:set>
<g:test1>foo: ${foo} one: ${one}</g:test1>
<% assert foo == 'Test' %>
'''
        assertOutputEquals('foo: bar one: 2', template, [:], { it.toString().trim() })
    }

    protected void onInit() {
        def tagClass = gcl.parseClass('''
class MyTagLib {
    def test1 = { attrs, body ->
        out << body(foo:"bar", one:2)
    }
}
''')
        grailsApplication.addArtefact(TagLibArtefactHandler.TYPE,tagClass)
    }
}
