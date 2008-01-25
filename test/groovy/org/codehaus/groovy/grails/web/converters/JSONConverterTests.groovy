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
import org.springframework.core.JdkVersion

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

        void testJSONEnumConverting() {
            if (JdkVersion.getMajorJavaVersion() >= JdkVersion.JAVA_15) {
                def enumClass = ga.classLoader.loadClass("Role")
                    enumClass.metaClass.asType = {java.lang.Class clazz ->
                    if (ConverterUtil.isConverterClass(clazz)) {
                        return ConverterUtil.createConverter(clazz, delegate)
                    } else {
                        return ConverterUtil.invokeOriginalAsTypeMethod(delegate, clazz)
                    }
                }
                def enumInstance = enumClass.HEAD

                def c = ga.getControllerClass("RestController").newInstance()

                c.params.e = enumInstance

                c.testEnum()

                assertEquals( '{"enumType":"Role","name":"HEAD"}', response.contentAsString)


            }
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

    def testEnum = {
        render params.e as JSON
    }
}
class Book {
    Long id
    Long version
    String title
    String author

}

        '''

                )

        if (JdkVersion.getMajorJavaVersion() >= JdkVersion.JAVA_15) {
            gcl.parseClass '''
public enum Role { HEAD, DISPATCHER, ADMIN }
'''
        }

    }


}