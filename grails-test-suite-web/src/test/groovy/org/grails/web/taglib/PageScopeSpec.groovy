package org.grails.web.taglib

import grails.test.mixin.TestMixin
import grails.test.mixin.web.GroovyPageUnitTestMixin
import spock.lang.Specification

/**
 * @author Graeme Rocher
 * @since 0.4
 */
@TestMixin(GroovyPageUnitTestMixin)
class PageScopeSpec extends Specification {

    void 'test referring to non existent page scope property does not throw MissingPropertyException'() {
        expect:
        applyTemplate("<%= pageScope.nonExistent ?: 'No Property Found' %>") == 'No Property Found'
    }
    
    void 'test page scope'() {

        expect:
        applyTemplate ('''\
<g:set var="one" scope="request" value="two" />\
<g:set var="two" scope="page" value="three" />\
<g:set var="three" scope="session" value="four" />\
one: ${request.one} two: ${two} three: ${session.three}\
''') == 'one: two two: three three: four'
    }
}
