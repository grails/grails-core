package org.codehaus.groovy.grails.web.taglib

import grails.util.GrailsUtil

import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException

/**
 * Tests some of the core tags when rendering inside GSP.
 *
 * @author Graeme Rocher
 * @since 0.6
 */
class CoreTagsTests extends AbstractGrailsTagTests {

    void testUnlessWithTestCondition() {
        def template = '<g:unless test="${cond}">body text</g:unless>'
        assertOutputEquals 'body text', template, [cond: false], {it.toString().trim()}
        assertOutputEquals '', template, [cond: true], {it.toString().trim()}
    }

    void testUnlessWithEnvCondition() {
        def template = '<g:unless env="production">body text</g:unless>'
        assertOutputEquals 'body text', template, [:], {it.toString().trim()}
        template = '<g:unless env="development">body text</g:unless>'
        assertOutputEquals '', template, [:], {it.toString().trim()}
    }

    void testUnlessWithEnvAndTestConditions() {
        def template = '<g:unless env="production" test="${cond}">body text</g:unless>'
        assertOutputEquals 'body text', template, [cond: false], {it.toString().trim()}
        assertOutputEquals 'body text', template, [cond: true], {it.toString().trim()}

        template = '<g:unless env="development" test="${cond}">body text</g:unless>'
        assertOutputEquals 'body text', template, [cond: false], {it.toString().trim()}
        assertOutputEquals '', template, [cond: true], {it.toString().trim()}
    }

    void testIfElse() {

        def template = '''
<g:if test="${foo}">foo</g:if>
<g:else>bar</g:else>
'''

        assertOutputEquals("foo", template, [foo:true], { it.toString().trim() })
        assertOutputEquals("bar", template, [foo:false], { it.toString().trim() })
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
        assertOutputEquals("", template, [foo:true], { it.toString().trim() })

        // Here we assume "development" is the env during tests
        def template2 = '''
<g:if env="development" test="${foo}">foo</g:if>
'''
        assertOutputEquals("foo", template2, [foo:true], { it.toString().trim() })
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
        assertOutputEquals("bar", template, [foo:false], { it.toString().trim() })

        template = '''
<g:if test="${foo}">foo</g:if>
<g:elseif test="${!foo}">bar</g:elseif>
'''
        assertOutputEquals("bar", template, [foo:false], { it.toString().trim() })

        template = '''
<g:if test="${foo}">foo</g:if>
<g:elseif test="${foo}" env="development">bar</g:elseif>
'''
        assertOutputEquals("", template, [foo:false], { it.toString().trim() })


        template = '''
<g:if test="${foo}">foo</g:if>
<g:elseif test="${!foo}" env="development">bar</g:elseif>
'''
        assertOutputEquals("bar", template, [foo:false], { it.toString().trim() })
    }
}
