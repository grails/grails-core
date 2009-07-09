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
import java.lang.reflect.Method
import org.springframework.validation.Errors
import org.springframework.validation.BeanPropertyBindingResult
import grails.converters.XML
import org.codehaus.groovy.grails.web.converters.marshaller.xml.ValidationErrorsMarshaller
import org.codehaus.groovy.grails.web.converters.marshaller.ProxyUnwrappingMarshaller
import org.hibernate.repackage.cglib.proxy.Enhancer
import org.hibernate.repackage.cglib.proxy.MethodProxy
import org.hibernate.proxy.LazyInitializer
import org.hibernate.proxy.HibernateProxy

class XMLConverterTests extends AbstractGrailsControllerTests {

       void testXMLConverter() {
           def c = ga.getControllerClass("RestController").newInstance()

           c.test()

           // @todo this test is fragile and depends on runtime environment because
           // of hash key ordering variations
           assertEquals( '''<?xml version="1.0" encoding="UTF-8"?><book><author>Stephen King</author><title>The Stand</title></book>''', response.contentAsString)
       }

       void testConvertErrors() {
           def valErrorMarshaller = new ValidationErrorsMarshaller()
           valErrorMarshaller.setApplicationContext(ctx)
           XML.registerObjectMarshaller(valErrorMarshaller)
           
           def c = ga.getControllerClass("RestController").newInstance()

           c.testErrors()

           // @todo this test is fragile and depends on runtime environment because
           // of hash key ordering variations

           def xml = new XmlSlurper().parseText(response.contentAsString)

           def titleError = xml.error.find { it.@field == 'title' }
           assertEquals 'Property [title] of class [class Book] cannot be null', titleError.message.text()
           def authorError = xml.error.find { it.@field == 'author' }
           assertEquals 'Property [author] of class [class Book] cannot be null', authorError.message.text()
          
       }

        void testProxiedDomainClassWithXMLConverter() {
            def obj = ga.getDomainClass("Book").newInstance()
            obj.title = "The Stand"
            obj.author = "Stephen King"
            def c = ga.getControllerClass("RestController").newInstance()

            def hibernateInitializer = [getImplementation:{obj}] as LazyInitializer
            def proxy = [getHibernateLazyInitializer:{hibernateInitializer}] as HibernateProxy

            c.params.b = proxy
            c.testProxy()

            def pum = new ProxyUnwrappingMarshaller()

            assertTrue pum.supports(proxy)
            // @todo this test is fragile and depends on runtime environment because
            // of hash key ordering variations
            println response.contentAsString
           assertEquals( '''<?xml version="1.0" encoding="UTF-8"?><book><author>Stephen King</author><title>The Stand</title></book>''', response.contentAsString)
        }

    void onSetUp() {
        GroovySystem.metaClassRegistry.removeMetaClass Errors
        GroovySystem.metaClassRegistry.removeMetaClass BeanPropertyBindingResult
        
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

  def testErrors = {
     def b = new Book()
     b.validate()
     render b.errors as XML
  }

}
class Book {
 Long id
 Long version
 String title
 String author

}''')
    }


}