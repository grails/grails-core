/**
 * Class description here.
 
 * @author Graeme Rocher
 * @since 0.4
  *
 * Created: Sep 4, 2007
 * Time: 1:34:06 PM
 * 
 */
package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.commons.TagLibArtefactHandler
import org.springframework.validation.MapBindingResult

class NamespacedTagLibMethodTests extends AbstractGrailsTagTests {

    void testStringAsBodyDispatch() {
        def template = '<g:tag bean="${foo}"/>'

        assertOutputEquals('errors', template,[foo:new NSTestBean()])
    }
    void testInvokeTagWithNamespace() {

        def template = '<my:test1>foo: <my:test2 foo="bar1" /> one: ${my.test2(foo:"bar2")} two: ${my.test2()}</my:test1>'

        assertOutputEquals('foo: hello! bar1 one: hello! bar2 two: hello! null', template)
    }

    void testInvokeTagWithNamespaceFromTagLib() {
        def template = '<my:test1>foo: <two:test1 /> </my:test1>'

        assertOutputEquals('foo: hello! bar3 ', template)
    }

    void testInvokeTagWithNonExistantNamespace() {
        def template = '''<foaf:Person a="b" c="d">foo</foaf:Person>'''

        println( applyTemplate(template) )
        // we don't have a 'foaf' namespace, so the output should be equal to template itself
        assertOutputEquals(template, template)

        // test with nested 'unknown' tags
        template = '''<foaf:Person a="b" c="d"><foaf:Nested e="f" g="h">Something here.</foaf:Nested></foaf:Person>'''

        println( applyTemplate(template) )
        assertOutputEquals(template, template)
    }

    void testInvokeDefaultNamespaceFromNamespacedTag() {
        def template = '''<alt:showme />'''

        

        assertOutputEquals("/test/foo", template )

        template = '''<alt:showmeToo />'''
        
        assertOutputEquals("/test/foo", template )

        template = '''<alt:showmeThree />'''

        assertOutputEquals("hello! bar", template )

    }

    void onInit() {
        def tagClass = gcl.parseClass( '''
class MyTagLib {
    static namespace = "my"
    def test1 = { attrs, body ->
        out << body(foo:"bar", one:2)
    }

    def test2 = { attrs, body ->
        out << "hello! ${attrs.foo}"
    }
}
''')
       def tagClass2 = gcl.parseClass('''
class SecondTagLib {
   static namespace = "two"

   def test1 = { attrs, body ->
        out << my.test2(foo:"bar3")          
   }

}
       ''')
       def tagClass3 = gcl.parseClass('''
class HasErrorTagLib {
    def tag = { attr, body ->
        out << hasErrors('bean': attr.bean, field: 'bar', 'errors')
    }
}
       ''')
       def tagClass4 = gcl.parseClass('''
class AlternateTagLib {
    static namespace = "alt"

    def showme = { attrs, body ->
        out << createLink(controller:'test',action:'foo')
    }

    def showmeToo = { attrs, body ->
        out << g.createLink(controller:'test',action:'foo')
    }

    def showmeThree = { attrs, body ->
       out << my.test2(foo:"bar")
    }
}
''')
        grailsApplication.addArtefact(TagLibArtefactHandler.TYPE,tagClass)
        grailsApplication.addArtefact(TagLibArtefactHandler.TYPE,tagClass2)
        grailsApplication.addArtefact(TagLibArtefactHandler.TYPE,tagClass3)
        grailsApplication.addArtefact(TagLibArtefactHandler.TYPE,tagClass4)
    }
}
class NSTestBean {
    def errors = [hasErrors:{true}, hasFieldErrors:{String name->true}] // mock errors object
    def hasErrors() { true }
}