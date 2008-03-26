/**
 * Created: Mar 26, 2008
 */
package org.codehaus.groovy.grails.web.pages

import org.codehaus.groovy.grails.web.taglib.AbstractGrailsTagTests

class GroovyPagesWhitespaceParsingTagTests extends AbstractGrailsTagTests {

    void testTagWithTab() {
        // there is a tab (\t) between "if" and test
        def template = '<g:if\ttest="${2 > 1}">rechoice</g:if>'

        assertOutputEquals "rechoice", template
    }

    void testTagWithSpace() {
        // there is a tab (\t) between "if" and test
        def template = '<g:if test="${2 > 1}">rechoice</g:if>'

        assertOutputEquals "rechoice", template
    }
    void testTagWithNewline() {
        // there is a tab (\t) between "if" and test
        def template = """<g:if
test="${2 > 1}">rechoice</g:if>"""

        assertOutputEquals "rechoice", template
    }


}
