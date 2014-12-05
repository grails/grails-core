package org.grails.web.pages

import grails.util.Environment
import org.grails.gsp.GroovyPagesException
import org.grails.web.taglib.AbstractGrailsTagTests

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

    void testForeachIteratingMap() {
        def template='<g:each var="k,v" in="[a:1,b:2,c:3]">${k}=${v},</g:each>'
        def result = applyTemplate(template, [:])
        assertEquals 'a=1,b=2,c=3,', result
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

    void testMultilineAttributeGRAILS8253() {
        def template='''<html>
<head>
<title>Sample onclick issue page</title>
</head>
<body>
<g:form name="testForm" controller="begin" action="create">
<g:textField name="testField"/>
<g:actionSubmit class="buttons" action="testAction" value="This
is a test action description"
onclick="if (testForm.testField.value=='') { alert('Please enter some text.'); return false; }"
/>
</g:form>
</body>
</html>'''
        def result=applyTemplate(template, [:])
        assertEquals '''<html>
<head>
<title>Sample onclick issue page</title>
</head>
<body>
<form action="/begin/create" method="post" name="testForm" id="testForm" >
<input type="text" name="testField" value="" id="testField" />
<input type="submit" name="_action_testAction" value="This
is a test action description" class="buttons" onclick="if (testForm.testField.value==&#39;&#39;) { alert(&#39;Please enter some text.&#39;); return false; }" />
</form>
</body>
</html>''', result
    }

    void testNestedExpression() {
        def template='''<g:set var="a" value="hello"/><g:set var="b" value='${[test: "${a} ${a}"]}'/>${b.test}'''
        def result = applyTemplate(template, [:])
        assertEquals 'hello hello', result
    }

    void testGstring() {
        def template='''<g:set var="a" value="hello"/><g:set var="b" value='${"${a} ${a}"}'/>${b}'''
        def result = applyTemplate(template, [:])
        assertEquals 'hello hello', result
    }

    void testGstring2() {
        def template='''<g:set var="a" value="hello"/><g:set var="b" value='${a} ${a}'/>${b}'''
        def result = applyTemplate(template, [:])
        assertEquals 'hello hello', result
    }

    void testGstring3() {
        def template='''<g:set var="a" value="hello"/><g:set var="b" value='${a} hello'/>${b}'''
        def result = applyTemplate(template, [:])
        assertEquals 'hello hello', result
    }

    void testGstring4() {
        def template='''<g:set var="a" value="hello"/><g:set var="b" value='hello ${a}'/>${b}'''
        def result = applyTemplate(template, [:])
        assertEquals 'hello hello', result
    }

    void testGstring5() {
        def template='''<g:set var="a" value="hello"/><g:set var="b" value='hello ${a} hello'/>${b}'''
        def result = applyTemplate(template, [:])
        assertEquals 'hello hello hello', result
    }

    void testNotGstring() {
        def template='''<g:set var="a" value="hello"/><g:set var="b" value="${'hello ${a} hello'}"/>${b}'''
        def result = applyTemplate(template, [:])
        assertEquals 'hello ${a} hello', result
    }

    void testNotGstring2() {
        def template='''<g:set var="a" value="hello"/><g:set var="b" value='${"hello \\${a} hello"}'/>${b}'''
        def result = applyTemplate(template, [:])
        assertEquals 'hello ${a} hello', result
    }

    void testNotGstring3() {
        def template='''<g:set var="a" value="hello"/><g:set var="b" value="${a + '${'}"/>${b}'''
        def result = applyTemplate(template, [:])
        assertEquals 'hello${', result
    }

    void testNestedExpressionInMap() {
        def template='''<g:set var="a" value="hello"/><g:set var="b" value='[test: "${a} ${a}"]'/>${b.test}'''
        def result = applyTemplate(template, [:])
        assertEquals 'hello hello', result
    }
}
