package org.grails.web.taglib

import org.grails.core.artefact.TagLibArtefactHandler

/**
 * @author Graeme Rocher
 * @since 0.4
 */
class PageScopeTagTests extends AbstractGrailsTagTests {

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
import grails.gsp.*

@TagLib
class MyTagLib {
    Closure test1 = { attrs, body ->
        pageScope.bar = "foo"
        out << pageScope.foo
    }
}
''')
        grailsApplication.addArtefact(TagLibArtefactHandler.TYPE,tagClass)
    }
}