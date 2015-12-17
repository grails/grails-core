package org.grails.web.converters

import grails.converters.JSON
import grails.converters.XML
import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin

import org.grails.web.converters.marshaller.json.DomainClassMarshaller as JsonClassMarshaller
import org.grails.web.converters.marshaller.xml.DomainClassMarshaller as XmlClassMarshaller
import org.grails.web.converters.marshaller.ClosureObjectMarshaller
import org.junit.Test
import static org.junit.Assert.assertEquals

/**
 * Tests for the customizable Converter Configuration.
 *
 * @author Siegfried Puchbauer
 */
@TestMixin(ControllerUnitTestMixin)
@Mock(ConverterBook)
class ConverterConfigurationTests {

    @Test
    void testCustomClosureMarshallerRegistration() {

        JSON.registerObjectMarshaller(ConverterBook) {
            [id: it.id,
             title: it.title,
             foo: 'bar'
            ]
        }

        def book = new ConverterBook()
        book.id = 4711
        book.title = "The Definitive Guide to Grails"

        assertEquals((book as JSON).toString(), """{"id":4711,"title":"The Definitive Guide to Grails","foo":"bar"}""")
    }

    @Test
    void testDefaultConverterConfigurationObjectMarshallerRegistration() {

        JSON.registerObjectMarshaller(java.sql.Date) { it.toString() }

        JSON.registerObjectMarshaller(java.sql.Time) { it.toString() }

        def objA = new java.sql.Date(System.currentTimeMillis())
        def objB = new java.sql.Time(System.currentTimeMillis())

        assertEquals(([a:objA] as JSON).toString(), """{"a":"${objA}"}""".toString())
        assertEquals(([b:objB] as JSON).toString(), """{"b":"${objB}"}""".toString())
    }

    @Test
    void testMarshallerRegistrationOrder() {

        JSON.registerObjectMarshaller(Date) { "FAIL" }

        JSON.registerObjectMarshaller(Date) { "SUCCESS" }

        assertEquals(([d: new Date()] as JSON).toString(), """{"d":"SUCCESS"}""")
    }

    @Test
    void testMarshallerPriority() {

        def om1 = new ClosureObjectMarshaller(ConverterWidget, { "SUCCESS" })
        def om2 = new ClosureObjectMarshaller(ConverterWidget, { "FAIL" })

        JSON.registerObjectMarshaller(om1, 5)
        JSON.registerObjectMarshaller(om2, 3)

        assertEquals(([d: new ConverterWidget()] as JSON).toString(), """{"d":"SUCCESS"}""")
    }

    @Test
    void testNamedConfigurations() {

        JSON.registerObjectMarshaller(Date) { "DEFAULT" }

        JSON.createNamedConfig("test-config") { cfg ->
            cfg.registerObjectMarshaller(Date) { "TEST" }
        }

        def obj = [d: new Date()]
        assertEquals((obj as JSON).toString(), """{"d":"DEFAULT"}""")

        JSON.use("test-config") {
            assertEquals((obj as JSON).toString(), """{"d":"TEST"}""")
        }

        assertEquals((obj as JSON).toString(), """{"d":"DEFAULT"}""")
    }

    @Test
    void testDomainWithVersionConfiguration() {

        JSON.createNamedConfig("with-version") {
            it.registerObjectMarshaller(new JsonClassMarshaller(true, grailsApplication))
        }

        XML.createNamedConfig("with-version") {
            it.registerObjectMarshaller(new XmlClassMarshaller(true, grailsApplication))
        }

        JSON.use("with-version") {
            assertEquals(
                """{"id":4711,"version":0,"author":"Graeme Rocher","title":"The Definitive Guide to Grails"}""",
                (createBook() as JSON).toString())
        }
        XML.use("with-version") {
            assertEquals(
                """<?xml version="1.0" encoding="UTF-8"?><converterBook id="4711" version="0"><author>Graeme Rocher</author><title>The Definitive Guide to Grails</title></converterBook>""",
                (createBook() as XML).toString())
        }
    }

//    @Test
//    void testPrettyPrintConfiguration() {
//
//        JSON.createNamedConfig("pretty-print") { cfg -> cfg.prettyPrint = true }
//        XML.createNamedConfig("pretty-print") { cfg -> cfg.prettyPrint = true }
//
//def prettyJSON = """{
//  "class": "org.grails.web.converters.ConverterBook",
//  "id": 4711,
//  "author": "Graeme Rocher",
//  "title": "The Definitive Guide to Grails"
//}"""
//
//        JSON.use("pretty-print") {
//            assertEquals(prettyJSON.replaceAll('[\r\n]', ''), (createBook() as JSON).toString().replaceAll('[\r\n]', ''))
//        }
//
//        def prettyXML = """<?xml version="1.0" encoding="UTF-8"?>
//<converterBook id="4711">
//  <author>
//    Graeme Rocher
//  </author>
//  <title>
//    The Definitive Guide to Grails
//  </title>
//</converterBook>"""
//        XML.use("pretty-print") {
//            assertEquals(prettyXML.replaceAll('[\r\n]', ''), (createBook() as XML).toString().replaceAll('[\r\n]', '').trim())
//        }
//    }

    protected createBook() {
        def book = new ConverterBook()
        book.id = 4711
        book.version = 0
        book.title = "The Definitive Guide to Grails"
        book.author = "Graeme Rocher"
        book
    }
}

@Entity
class ConverterBook {
    String title
    String author
}

class ConverterWidget{}
