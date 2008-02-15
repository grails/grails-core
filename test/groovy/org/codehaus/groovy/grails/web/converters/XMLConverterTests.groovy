/**
 * Tests for the XML converter
 
 * @author Graeme Rocher
 * @since 0.6
  *
 * Created: Aug 3, 2007
 * Time: 6:50:09 PM
 * 
 */

package org.codehaus.groovy.grails.web.converters

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import net.sf.cglib.proxy.Enhancer
import java.lang.reflect.Method
import net.sf.cglib.proxy.MethodProxy
import net.sf.cglib.proxy.MethodInterceptor

class XMLConverterTests extends AbstractGrailsControllerTests {

       void testXMLConverter() {
           def c = ga.getControllerClass("RestController").newInstance()

           c.test()

           assertEquals( '''<?xml version="1.0" encoding="utf-8"?><book>
  <author>Stephen King</author>
  <title>The Stand</title>
</book>''', response.contentAsString)
       }

        void testProxiedDomainClassWithXMLConverter() {
            Enhancer e = new Enhancer();

            e.superclass = ga.getDomainClass("Book").clazz
            e.callback = { obj, Method method, Object[] args,
                               MethodProxy proxy ->
                proxy.invokeSuper obj, args

            } as MethodInterceptor

            def proxy = e.create()
            proxy.title = "The Stand"
            proxy.author = "Stephen King"
            def c = ga.getControllerClass("RestController").newInstance()

            c.params.b = proxy

            c.testProxy()
            // todo: Get Grails' Xstream stuff to deal with CGlib proxies
           assertEquals( '''<?xml version="1.0" encoding="utf-8"?><CGLIB-enhanced-proxy>
  <author>Stephen King</author>
  <title>The Stand</title>
</CGLIB-enhanced-proxy>''', response.contentAsString)
        }

    void onSetUp() {
        gcl.parseClass('''
import grails.converters.*
        
class RestController {
     def test = {
        def b = new Book(title:'The Stand', author:'Stephen King')
        render b as XML
     }

     def testProxy = {
        render params.b as XML
     }

}
class Book {
    Long id
    Long version
    String title
    String author

}

        ''')


    }


}