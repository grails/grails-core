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

    void testForeachInTagbody() {
        def template='<g:set var="p"><g:each in="${toplist}"><g:each var="t" in="${it.sublist}">${t}</g:each></g:each></g:set>${p}'
        def result = applyTemplate(template, [toplist: [[sublist:['a','b']],[sublist:['c','d']]]])
        assertEquals 'abcd', result
    }

    void testForeachRenaming() {
        def template='<g:each in="${list}"><g:each in="${list}">.</g:each></g:each>'
        def result=applyTemplate(template, [list: 1..10])
        assertEquals '.' * 100, result
    }

    void testForeachGRAILS8089() {
        def template='''<g:each in="${mockGrailsApplication.domainClasses.findAll{it.clazz=='we' && (it.clazz != 'no')}.sort({a,b->a.fullName.compareTo(b.fullName)})}"><option value="${it.fullName}"><g:message code="content.item.name.${it.fullName}" encodeAs="HTML"/></option></g:each>'''
        def result=applyTemplate(template, [mockGrailsApplication: [domainClasses: [[fullName: 'MyClass2', clazz:'we'], [fullName: 'MyClass1', clazz:'we'], [fullName: 'MyClass3', clazz:'no']] ]])
        assertEquals '<option value="MyClass1">content.item.name.MyClass1</option><option value="MyClass2">content.item.name.MyClass2</option>', result
    }
}
