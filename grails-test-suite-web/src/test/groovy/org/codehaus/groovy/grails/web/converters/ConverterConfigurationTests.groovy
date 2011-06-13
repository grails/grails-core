package org.codehaus.groovy.grails.web.converters

import grails.converters.JSON
import grails.converters.XML

import org.codehaus.groovy.grails.web.converters.marshaller.ClosureOjectMarshaller
import org.codehaus.groovy.grails.web.converters.marshaller.json.DomainClassMarshaller as JsonClassMarshaller
import org.codehaus.groovy.grails.web.converters.marshaller.xml.DomainClassMarshaller as XmlClassMarshaller
import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests

/**
 * Tests for the customizable Converter Configuration.
 *
 * @author Siegfried Puchbauer
 */
class ConverterConfigurationTests extends AbstractGrailsControllerTests {

    void testCustomClosureMarshallerRegistration() {

        def bookClass = ga.getDomainClass("Book").clazz

        JSON.registerObjectMarshaller(bookClass) {
            [id: it.id,
             title: it.title,
             foo: 'bar'
            ]
        }

        def book = bookClass.newInstance()
        book.id = 4711
        book.title = "The Definitive Guide to Grails"

        assertEquals((book as JSON).toString(), """{"id":4711,"title":"The Definitive Guide to Grails","foo":"bar"}""")
    }

    void testDefaultConverterConfigurationObjectMarshallerRegistration() {

        JSON.registerObjectMarshaller(java.sql.Date) { it.toString() }

        JSON.registerObjectMarshaller(java.sql.Time) { it.toString() }

        def objA = new java.sql.Date(System.currentTimeMillis())
        def objB = new java.sql.Time(System.currentTimeMillis())

        assertEquals(([a:objA] as JSON).toString(), """{"a":"${objA}"}""")
        assertEquals(([b:objB] as JSON).toString(), """{"b":"${objB}"}""")
    }

    void testMarshallerRegistrationOrder() {

        JSON.registerObjectMarshaller(Date) { "FAIL" }

        JSON.registerObjectMarshaller(Date) { "SUCCESS" }

        assertEquals(([d: new Date()] as JSON).toString(), """{"d":"SUCCESS"}""")
    }

    void testMarshallerPriority() {

        def om1 = new ClosureOjectMarshaller(Date, { "SUCCESS" })
        def om2 = new ClosureOjectMarshaller(Date, { "FAIL" })

        JSON.registerObjectMarshaller(om1, 5)
        JSON.registerObjectMarshaller(om2, 3)

        assertEquals(([d: new Date()] as JSON).toString(), """{"d":"SUCCESS"}""")
    }

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

    void testDomainWithVersionConfiguration() {

        JSON.createNamedConfig("with-version") {
            it.registerObjectMarshaller(new JsonClassMarshaller(true, ga))
        }

        XML.createNamedConfig("with-version") {
            it.registerObjectMarshaller(new XmlClassMarshaller(true, ga))
        }

        JSON.use("with-version") {
            assertEquals(
                """{"class":"Book","id":4711,"version":0,"author":"Graeme Rocher","title":"The Definitive Guide to Grails"}""",
                (createBook() as JSON).toString())
        }
        XML.use("with-version") {
            assertEquals(
                """<?xml version="1.0" encoding="UTF-8"?><book id="4711" version="0"><author>Graeme Rocher</author><title>The Definitive Guide to Grails</title></book>""",
                (createBook() as XML).toString())
        }
    }

    void testPrettyPrintConfiguration() {

        JSON.createNamedConfig("pretty-print") { cfg -> cfg.prettyPrint = true }
        XML.createNamedConfig("pretty-print") { cfg -> cfg.prettyPrint = true }

def prettyJSON = """{
  "class": "Book",
  "id": 4711,
  "author": "Graeme Rocher",
  "title": "The Definitive Guide to Grails"
}"""

        JSON.use("pretty-print") {
            assertEquals(prettyJSON, (createBook() as JSON).toString())
        }

        def prettyXML = """<?xml version="1.0" encoding="UTF-8"?>
<book id="4711">
  <author>
    Graeme Rocher
  </author>
  <title>
    The Definitive Guide to Grails
  </title>
</book>"""
        XML.use("pretty-print") {
            assertEquals(prettyXML, (createBook() as XML).toString().trim())
        }
    }

    protected createBook() {
        def book = ga.getDomainClass("Book").clazz.newInstance()
        book.id = 4711
        book.version = 0
        book.title = "The Definitive Guide to Grails"
        book.author = "Graeme Rocher"
        book
    }

    protected void onSetUp() {
        gcl.parseClass """
            @grails.persistence.Entity
            class Book {
               Long id
               Long version
               String title
               String author

            }
        """
    }
}
