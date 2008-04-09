/**
 * Class description here.
 
 * @author Graeme Rocher
 * @since 0.4
  *
 * Created: Sep 4, 2007
 * Time: 11:08:48 AM
 * 
 */
package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.commons.TagLibArtefactHandler

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
        assertOutputEquals('foo: bar one: 2', template)
    }

    void onInit() {
        def tagClass = gcl.parseClass( '''
class MyTagLib {
    def test1 = { attrs, body ->
        out << body(foo:"bar", one:2)
    }
}
''')
        grailsApplication.addArtefact(TagLibArtefactHandler.TYPE,tagClass)
    }
}