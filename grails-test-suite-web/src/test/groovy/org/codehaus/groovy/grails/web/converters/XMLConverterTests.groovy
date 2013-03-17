package org.codehaus.groovy.grails.web.converters

import grails.artefact.Artefact
import grails.converters.XML
import grails.persistence.Entity

import org.codehaus.groovy.grails.web.converters.marshaller.ProxyUnwrappingMarshaller
import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.codehaus.groovy.grails.web.servlet.mvc.HibernateProxy
import org.codehaus.groovy.grails.web.servlet.mvc.LazyInitializer
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors

 /**
 * Tests for the XML converter.
 *
 * @author Graeme Rocher
 * @since 0.6
 */
class XMLConverterTests extends AbstractGrailsControllerTests {

    @Override
    protected Collection<Class> getControllerClasses() {
        [RestController]
    }

    @Override
    protected Collection<Class> getDomainClasses() {
        [XmlConverterTestBook, XmlConverterTestPublisher, XmlConverterTestBookData]
    }

    void testXMLConverter() {
        def c = new RestController()
        c.test()

        // @todo this test is fragile and depends on runtime environment because of hash key ordering variations
        assertEquals '''<?xml version="1.0" encoding="UTF-8"?><xmlConverterTestBook><author>Stephen King</author><publisher /><title>The Stand</title></xmlConverterTestBook>''', response.contentAsString
    }

    void testXMLConverterWithByteArray() {
        def c = new RestController()
        c.testByteArray()

        assertEquals '''<?xml version="1.0" encoding="UTF-8"?><xmlConverterTestBookData><data encoding="BASE-64">''' +
                'It was the best of times, it was the worst...'.getBytes('UTF-8').encodeBase64() +
                '''</data></xmlConverterTestBookData>''', response.contentAsString
    }

    void testConvertErrors() {
        def c = new RestController()
        c.testErrors()

        // @todo this test is fragile and depends on runtime environment because
        // of hash key ordering variations

        println response.contentAsString

        def xml = new XmlSlurper().parseText(response.contentAsString)

        def titleError = xml.error.find { it.@field == 'title' }
        assertEquals "Property [title] of class [class ${XmlConverterTestBook.name}] cannot be null", titleError.message.text()
        def authorError = xml.error.find { it.@field == 'author' }
        assertEquals "Property [author] of class [class ${XmlConverterTestBook.name}] cannot be null", authorError.message.text()
    }

    void testProxiedDomainClassWithXMLConverter() {
        def obj = new XmlConverterTestBook()
        obj.title = "The Stand"
        obj.author = "Stephen King"
        def c = new RestController()

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

        def obj = new XmlConverterTestPublisher()
        obj.name = "Apress"
        obj.id = 1L

        def book = new XmlConverterTestBook()
        book.title = "The Stand"
        book.author = "Stephen King"
        book.publisher = obj

        def c = new RestController()
        c.params.b = book
        c.testProxyAssociations()

        assertEquals('''<?xml version="1.0" encoding="UTF-8"?><xmlConverterTestBook><author>Stephen King</author><publisher id="1" /><title>The Stand</title></xmlConverterTestBook>''', response.contentAsString)
    }

    void onSetUp() {
        GroovySystem.metaClassRegistry.removeMetaClass Errors
        GroovySystem.metaClassRegistry.removeMetaClass BeanPropertyBindingResult
    }
}

@Artefact("Controller")
class RestController {
    def test = {
        def b = new XmlConverterTestBook(title:'The Stand', author:'Stephen King')
        render b as XML
    }

    def testByteArray = {
        def b = new XmlConverterTestBookData(data: 'It was the best of times, it was the worst...'.getBytes('UTF-8'))
        render b as XML
    }

    def testProxy = { render params.b as XML }

    def testProxyAssociations = { render params.b as XML }

    def testErrors = {
        def b = new XmlConverterTestBook()
        b.validate()
        render b.errors as XML
    }
}

@Entity
class XmlConverterTestBook {
    Long id
    Long version
    String title
    String author

    XmlConverterTestPublisher publisher
}

@Entity
class XmlConverterTestPublisher {
    Long id
    Long version
    String name
}

@Entity
class XmlConverterTestBookData {
    Long id
    Long version
    byte[] data
}
