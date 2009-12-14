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

import org.springframework.core.JdkVersion
import org.springframework.validation.Errors
import org.springframework.validation.BeanPropertyBindingResult
import org.hibernate.proxy.LazyInitializer
import org.hibernate.proxy.HibernateProxy
import grails.converters.JSON

class JSONConverterTests extends AbstractGrailsControllerTests {

       void testNullJSONValues() {
           def c = ga.getControllerClass("RestController").newInstance()
           c.testNullValues()

           assertEquals( '{}', response.contentAsString)
       }

       void testJSONConverter() {
           def c = ga.getControllerClass("RestController").newInstance()

           c.test()

           // @todo this test is fragile and depends on runtime environment because
           // of hash key ordering variations
           println ">> $response.contentAsString)."
           assertEquals( '''{"class":"Book","id":null,"author":"Stephen King","title":"The Stand"}''', response.contentAsString)
       }

       void testConvertErrors() {
           def c = ga.getControllerClass("RestController").newInstance()

           c.testErrors()

           // @todo this test is fragile and depends on runtime environment because
           // of hash key ordering variations
           def json = JSON.parse(response.contentAsString)

           def titleError = json.errors.find { it.field == 'title' }

            assertEquals "Property [title] of class [class Book] cannot be null", titleError.message
            def authorError = json.errors.find { it.field == 'author' }

            assertEquals "Property [author] of class [class Book] cannot be null", authorError.message

       }

        void testProxiedDomainClassWithJSONConverter() {


            def obj = ga.getDomainClass("Book").newInstance()
            obj.title = "The Stand"
            obj.author = "Stephen King"
            def c = ga.getControllerClass("RestController").newInstance()

            def hibernateInitializer = [getImplementation:{obj}] as LazyInitializer
            def proxy = [getHibernateLazyInitializer:{hibernateInitializer}] as HibernateProxy

            c.params.b = proxy

            c.testProxy()

            // @todo this test is fragile and depends on runtime environment because
            // of hash key ordering variations
            assertEquals( '''{"class":"Book","id":null,"author":"Stephen King","title":"The Stand"}''', response.contentAsString)
        }

        void testJSONEnumConverting() {
            if (JdkVersion.isAtLeastJava15()) {
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

                // @todo this test is fragile and depends on runtime environment because
                // of hash key ordering variations
                assertEquals( '{"enumType":"Role","name":"HEAD"}', response.contentAsString)


            }
        }

    void onSetUp() {
        println "JSONConverterTests.onSetUp()"
        GroovySystem.metaClassRegistry.removeMetaClass Errors
        GroovySystem.metaClassRegistry.removeMetaClass BeanPropertyBindingResult

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

    def testErrors = {
        def b = new Book()
        b.validate()
        render b.errors as JSON
    }

   def testEnum = {
       render params.e as JSON
   }

    def testNullValues = {
        def descriptors = [:]
        descriptors.put(null,null)
        render descriptors as JSON
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

        if (JdkVersion.isAtLeastJava15()) {
            gcl.parseClass '''
public enum Role { HEAD, DISPATCHER, ADMIN }
'''
        }

    }


}