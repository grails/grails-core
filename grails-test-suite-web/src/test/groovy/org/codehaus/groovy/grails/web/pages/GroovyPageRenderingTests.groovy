package org.codehaus.groovy.grails.web.pages

import org.codehaus.groovy.grails.web.taglib.AbstractGrailsTagTests
import org.codehaus.groovy.grails.web.pages.exceptions.GroovyPagesException
import grails.util.Environment

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class GroovyPageRenderingTests extends AbstractGrailsTagTests {

    void testGroovyPageExpressionExceptionInDevelopmentEnvironment() {
        def template = '${foo.bar.next}'

        shouldFail(GroovyPagesException) {
            applyTemplate(template)
        }
    }

    void testGroovyPageExpressionExceptionInOtherEnvironments() {
        def template = '${foo.bar.next}'

        System.setProperty(Environment.KEY, "production")

        shouldFail(NullPointerException) {
            applyTemplate(template)
        }
    }

    protected void onDestroy() {
        System.setProperty(Environment.KEY, "")
    }

    void testForeach() {
        def template='<g:each in="${toplist}"><g:each var="t" in="${it.sublist}">${t}</g:each></g:each>'
        def result = applyTemplate(template, [toplist: [[sublist:['a','b']],[sublist:['c','d']]]])
        assertEquals 'abcd', result
    }


    void testForeachRenaming() {
        def template='<g:each in="${list}"><g:each in="${list}">.</g:each></g:each>'
        def result=applyTemplate(template, [list: 1..10])
        assertEquals '.' * 100, result
    }
}
