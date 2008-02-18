/**
 * Tests some of the core tags when rendering inside GSP
 
 * @author Graeme Rocher
 * @since 0.6
  *
 * Created: Jul 30, 2007
 * Time: 2:35:30 PM
 * 
 */

package org.codehaus.groovy.grails.web.taglib

import grails.util.GrailsUtil

import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException

class CoreTagsTests extends AbstractGrailsTagTests {

    void testIfElse() {


        def template = '''
<g:if test="${foo}">foo</g:if>
<g:else>bar</g:else>
'''

        assertOutputEquals("foo", template, [foo:true])
        assertOutputEquals("bar", template, [foo:false])
    }

   void testIfElseWithSpace() {
       def template = '''
<g:if test="${foo}">
foo
</g:if>

<g:else>
bar
</g:else>
   '''

       assertOutputEquals("foo", template, [foo:true]) { it.toString().trim() }
       assertOutputEquals("bar", template, [foo:false]) { it.toString().trim() }

   }

    void testIfWithEnv() {

        def template = '''
<g:if env="testing" test="${foo}">foo</g:if>
'''
        assertOutputEquals("", template, [foo:true])

        // Here we assume "development" is the env during tests
        def template2 = '''
<g:if env="development" test="${foo}">foo</g:if>
'''
        assertOutputEquals("foo", template2, [foo:true])
    }

    void testIfWithEnvAndWithoutTestAttribute() {
        def template = '''<g:if env="development">foo</g:if>'''
        assertOutputEquals("foo", template)
    }

    void testIfWithoutEnvAndTestAttributes() {  
        shouldFail(GrailsTagException) {
            applyTemplate("<g:if>foo</g:if>")
        }
    }

    void testElseIf() {

        def template = '''
<g:if test="${foo}">foo</g:if>
<g:elseif env="development">bar</g:elseif>
'''
        assertOutputEquals("bar", template, [foo:false])

        template = '''
<g:if test="${foo}">foo</g:if>
<g:elseif test="${!foo}">bar</g:elseif>
'''
        assertOutputEquals("bar", template, [foo:false])

        template = '''
<g:if test="${foo}">foo</g:if>
<g:elseif test="${foo}" env="development">bar</g:elseif>
'''
        assertOutputEquals("", template, [foo:false])


        template = '''
<g:if test="${foo}">foo</g:if>
<g:elseif test="${!foo}" env="development">bar</g:elseif>
'''
        assertOutputEquals("bar", template, [foo:false])

    }
}