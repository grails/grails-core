package org.codehaus.groovy.grails.web.pages

import org.codehaus.groovy.grails.web.taglib.AbstractGrailsTagTests

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Feb 5, 2009
 */

public class GroovyPageLineNumberTests extends AbstractGrailsTagTests{

    void testSpanningMultipleLines() {
        def template = '''
 <a href="${createLink(action:'listActivity', controller:'activity',
        params:[sort:params.sort?params.sort:'',
        order:params.order?params.order:'asc', offset:params.offset?params.offset:0])}"">Click me</a>
'''

        printCompiledSource template

        applyTemplate(template)
    }

    void testExpressionWithQuotes() {
        def template = '${foo + \' \' + bar}'

        assertOutputEquals "one two", template, [foo:"one", bar:"two"]

        template = '<g:createLinkTo dir="${foo}" file="${foo + \' \' + bar}" />'

        assertOutputEquals "/one/one two", template, [foo:"one", bar:"two"]


        template = '<g:link name="blah" action="${remoteFunction(action:\'bar\', params:\'\\\'grp=\\\' + encodeURIComponent(this.value)\')}"></g:link>'

        printCompiledSource template

        // test will fail if compilation fails
        applyTemplate(template)
    }
    void testLineNumberDataInsideTagAttribute() {
        def template = '''

<p />

<g:set var="foo" value="${foo.bar.path}" />

<p />
'''
        printCompiledSource template 
        try {
            applyTemplate(template)
        }
        catch (org.codehaus.groovy.grails.web.pages.exceptions.GroovyPagesException e) {
            def cause = e.cause
            while(cause != cause.cause && cause.cause) {
                cause = cause.cause
            }
            assertTrue "The cause should have been a NPE but was ${cause}", cause instanceof NullPointerException
            assertEquals 5,e.lineNumber
            
        }



    }

    void testLineNumberingDataInsideExpression() {

        def template = '''

<p />

${foo.bar.path}

<p />
'''
        try {
            applyTemplate(template)
        }
        catch (org.codehaus.groovy.grails.web.pages.exceptions.GroovyPagesException e) {

            def cause = e.cause
            while(cause != cause.cause && cause.cause) {
                cause = cause.cause
            }
            assertTrue "The cause should have been a NPE but was ${cause}", cause instanceof NullPointerException
            assertEquals 5,e.lineNumber
        }

    }

    void testEachWithQuestionMarkAtEnd() {
        def template = '<g:each in="${list?}">${it}</g:each>'

        assertOutputEquals "123", template, [list:[1,2,3]]
    }

    void testStringWithQuestionMark() {
        def template = '${"hello?"}'

        assertOutputEquals "hello?", template

    }
    void testComplexPage() {
        def template = '''
<html>
    <head>
        <title>Welcome to Grails</title>
		<meta name="layout" content="main" />
    </head>
    <body>
        <h1 style="margin-left:20px;">Welcome to Grails</h1>
		${foo.bar.suck}
        <p style="margin-left:20px;width:80%">Congratulations, you have successfully started your first Grails application! At the moment
        this is the default page, feel free to modify it to either redirect to a controller or display whatever
        content you may choose. Below is a list of controllers that are currently deployed in this application,
        click on each to execute its default action:</p>
        <div class="dialog" style="margin-left:20px;width:60%;">
            <ul>

              <g:each var="c" in="${grailsApplication.controllerClasses}">
                    <li class="controller"><g:link controller="${c.logicalPropertyName}">${c.fullName}</g:link></li>
              </g:each>
            </ul>

        </div>
    </body>
</html>
'''
        try {
            applyTemplate(template)
        }
        catch (org.codehaus.groovy.grails.web.pages.exceptions.GroovyPagesException e) {

            assertEquals 9,e.lineNumber
        }


    }

}