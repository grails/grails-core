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

class NamespacedTagLibMethodTests extends AbstractGrailsTagTests {

    void testInvokeTagWithNamespace() {

        def template = '<my:test1>foo: <my:test2 foo="bar1" /> one: ${my.test2(foo:"bar2")} two: ${my.test2()}</my:test1>'

        assertOutputEquals('foo: hello! bar1 one: hello! bar2 two: hello! null', template)
    }

    void testInvokeTagWithNamespaceFromTagLib() {
        def template = '<my:test1>foo: <two:test1 /> </my:test1>'

        assertOutputEquals('foo: hello! bar3', template)
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
        grailsApplication.addArtefact(TagLibArtefactHandler.TYPE,tagClass)
        grailsApplication.addArtefact(TagLibArtefactHandler.TYPE,tagClass2)
    }
}