package org.grails.web.converters

import grails.artefact.Artefact
import grails.converters.XML
import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestFor

import org.grails.web.converters.marshaller.ProxyUnwrappingMarshaller
import org.grails.web.servlet.mvc.HibernateProxy
import org.grails.web.servlet.mvc.LazyInitializer
import org.junit.Test
import static org.junit.Assert.*

/**
 * Tests for the XML converter.
 *
 * @author Graeme Rocher
 * @since 0.6
 */
@TestFor(RestController)
@Mock([XmlConverterTestBook, XmlConverterTestPublisher, XmlConverterTestBookData])
class XMLConverterTests {

    @Test
    void testXMLConverter() {
        controller.test()

        // @todo this test is fragile and depends on runtime environment because of hash key ordering variations
        assertEquals '''<?xml version="1.0" encoding="UTF-8"?><xmlConverterTestBook><author>Stephen King</author><publisher /><title>The Stand</title></xmlConverterTestBook>''', response.contentAsString
    }

    @Test
    void testXMLConverterWithByteArray() {
        controller.testByteArray()

        assertEquals '''<?xml version="1.0" encoding="UTF-8"?><xmlConverterTestBookData><data encoding="BASE-64">''' +
                'It was the best of times, it was the worst...'.getBytes('UTF-8').encodeBase64() +
                '''</data></xmlConverterTestBookData>''', response.contentAsString
    }

    @Test
    void testConvertErrors() {
        controller.testErrors()

        // @todo this test is fragile and depends on runtime environment because
        // of hash key ordering variations

        println response.contentAsString

        def xml = new XmlSlurper().parseText(response.contentAsString)

        def titleError = xml.error.find { it.@field == 'title' }
        assertEquals "Property [title] of class [class ${XmlConverterTestBook.name}] cannot be null".toString(), titleError.message.text()
        def authorError = xml.error.find { it.@field == 'author' }
        assertEquals "Property [author] of class [class ${XmlConverterTestBook.name}] cannot be null".toString(), authorError.message.text()
    }

    @Test
    void testProxiedDomainClassWithXMLConverter() {
        def obj = new XmlConverterTestBook()
        obj.title = "The Stand"
        obj.author = "Stephen King"

        def hibernateInitializer = [getImplementation:{obj}] as LazyInitializer
        def proxy = [getHibernateLazyInitializer:{hibernateInitializer}] as HibernateProxy
        params.b = proxy

        controller.testProxy()

        def pum = new ProxyUnwrappingMarshaller()

        assertTrue pum.supports(proxy)
        // @todo this test is fragile and depends on runtime environment because
        // of hash key ordering variations
        assertEquals('''<?xml version="1.0" encoding="UTF-8"?><xmlConverterTestBook><author>Stephen King</author><publisher /><title>The Stand</title></xmlConverterTestBook>''', response.contentAsString)
    }

    @Test
    void testMarshalProxiedAssociations() {

        def obj = new XmlConverterTestPublisher()
        obj.name = "Apress"
        obj.id = 1L

        def book = new XmlConverterTestBook()
        book.title = "The Stand"
        book.author = "Stephen King"
        book.publisher = obj

        params.b = book
        controller.testProxyAssociations()

        assertEquals('''<?xml version="1.0" encoding="UTF-8"?><xmlConverterTestBook><author>Stephen King</author><publisher id="1" /><title>The Stand</title></xmlConverterTestBook>''', response.contentAsString)
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
