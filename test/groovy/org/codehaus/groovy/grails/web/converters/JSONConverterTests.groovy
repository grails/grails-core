/**
 * Tests for the JSON converter.
 
 * @author Graeme Rocher
 * @since 0.4
  *
 * Created: Aug 3, 2007
 * Time: 9:10:19 PM
 * 
 */

package org.codehaus.groovy.grails.web.converters

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import net.sf.cglib.proxy.Enhancer
import net.sf.cglib.proxy.MethodInterceptor
import java.lang.reflect.Method
import net.sf.cglib.proxy.MethodProxy

class JSONConverterTests extends AbstractGrailsControllerTests {

       void testJSONConverter() {
           def c = ga.getControllerClass("RestController").newInstance()

           c.test()

           assertEquals( '''{"id":null,"class":"Book","author":"Stephen King","title":"The Stand"}''', response.contentAsString)
       }

        void testProxiedDomainClassWithJSONConverter() {
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

            assertEquals( '''{"id":null,"class":"Book","author":"Stephen King","title":"The Stand"}''', response.contentAsString)
        }

    void onSetUp() {
        gcl.parseClass('''
import grails.converters.*

class RestController {
     def test = {
        def b = new Book(title:'The Stand', author:'Stephen King')
        render b as JSON
     }

     def testProxy = {
        render params.b as JSON
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