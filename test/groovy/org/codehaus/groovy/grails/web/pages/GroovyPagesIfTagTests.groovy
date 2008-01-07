/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jan 7, 2008
 */
package org.codehaus.groovy.grails.web.pages

import org.codehaus.groovy.grails.web.taglib.AbstractGrailsTagTests

class GroovyPagesIfTagTests extends AbstractGrailsTagTests {

    void testGreaterThan() {
        def template = '<g:if test="${2 > 1}">rechoice</g:if>'

        assertOutputEquals "rechoice", template
    }

    void testComplexNestedGreaterThan() {
        def template = '<g:if test="${[1, 2, 3, 4].sum() { it * 2 } - [2, 3, 4, 5].sum() { (0..it).sum() { it * 2 } } > 0}">hello</g:if><g:else>goodbye</g:else>'

        printCompiledSource template

        assertCompiledSourceContains "if([1, 2, 3, 4].sum() { it * 2 } - [2, 3, 4, 5].sum() { (0..it).sum() { it * 2 } } > 0) {", template
        assertOutputEquals "goodbye", template
    }

}