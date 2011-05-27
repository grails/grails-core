package org.codehaus.groovy.grails.web.converters

import org.codehaus.groovy.grails.web.converters.marshaller.ProxyUnwrappingMarshaller
import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.hibernate.proxy.HibernateProxy
import org.hibernate.proxy.LazyInitializer
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors

 /**
 * Tests for the XML converter.
 *
 * @author Graeme Rocher
 * @since 0.6
 */
class XMLConverterTests extends AbstractGrailsControllerTests {

    void testXMLConverter() {
        def c = ga.getControllerClass("RestController").newInstance()
        c.test()

        // @todo this test is fragile and depends on runtime environment because
        // of hash key ordering variations
        assertEquals '''<?xml version="1.0" encoding="UTF-8"?><xmlConverterTestBook><author>Stephen King</author><publisher /><title>The Stand</title></xmlConverterTestBook>''', response.contentAsString
    }

    void testConvertErrors() {
        def c = ga.getControllerClass("RestController").newInstance()
        c.testErrors()

        // @todo this test is fragile and depends on runtime environment because
        // of hash key ordering variations

        println response.contentAsString

        def xml = new XmlSlurper().parseText(response.contentAsString)

        def titleError = xml.error.find { it.@field == 'title' }
        assertEquals 'Property [title] of class [class XmlConverterTestBook] cannot be null', titleError.message.text()
        def authorError = xml.error.find { it.@field == 'author' }
        assertEquals 'Property [author] of class [class XmlConverterTestBook] cannot be null', authorError.message.text()
    }

    void testProxiedDomainClassWithXMLConverter() {
        def obj = ga.getDomainClass("XmlConverterTestBook").newInstance()
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
        assertEquals('''<?xml version="1.0" encoding="UTF-8"?><xmlConverterTestBook><author>Stephen King</author><publisher /><title>The Stand</title></xmlConverterTestBook>''', response.contentAsString)
    }

    void testMarshalProxiedAssociations() {

        def obj = ga.getDomainClass("XmlConverterTestPublisher").newInstance()
        obj.name = "Apress"
        obj.id = 1L

        def hibernateInitializer = [getImplementation:{obj},getPersistentClass:{obj.getClass()}] as LazyInitializer
        def proxy = [getHibernateLazyInitializer:{hibernateInitializer}] as HibernateProxy

        def book = ga.getDomainClass("XmlConverterTestBook").newInstance()
        book.title = "The Stand"
        book.author = "Stephen King"
        book.publisher = obj

        def c = ga.getControllerClass("RestController").newInstance()
        c.params.b = book
        c.testProxyAssociations()

        assertEquals('''<?xml version="1.0" encoding="UTF-8"?><xmlConverterTestBook><author>Stephen King</author><publisher id="1" /><title>The Stand</title></xmlConverterTestBook>''', response.contentAsString)
    }

    void onSetUp() {
        GroovySystem.metaClassRegistry.removeMetaClass Errors
        GroovySystem.metaClassRegistry.removeMetaClass BeanPropertyBindingResult

        gcl.parseClass '''
import grails.converters.*

@grails.artefact.Artefact("Controller")
class RestController {
  def test = {
     def b = new XmlConverterTestBook(title:'The Stand', author:'Stephen King')
     render b as XML
  }

  def testProxy = {
     render params.b as XML
  }

  def testProxyAssociations = {
        render params.b as XML
  }

  def testErrors = {
     def b = new XmlConverterTestBook()
     b.validate()
     render b.errors as XML
  }

}

@grails.persistence.Entity
class XmlConverterTestBook {
 Long id
 Long version
 String title
 String author

 XmlConverterTestPublisher publisher

}
@grails.persistence.Entity
class XmlConverterTestPublisher {
 Long id
 Long version
 String name
}
'''
    }
}
