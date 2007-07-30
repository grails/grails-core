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


}