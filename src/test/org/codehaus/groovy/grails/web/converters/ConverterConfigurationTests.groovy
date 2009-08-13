package org.codehaus.groovy.grails.web.converters

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import grails.converters.JSON
import org.codehaus.groovy.grails.web.converters.configuration.DefaultConverterConfiguration

/**
 * Tests for the customizable Converter Configuration
 *
 * @author Siegfried Puchbauer
 */
class ConverterConfigurationTests extends AbstractGrailsControllerTests {

    void testCustomClosureMarshallerRegistration() {

        def bookClass = ga.getDomainClass("Book").clazz

        JSON.registerObjectMarshaller(bookClass) {
            [
                    id: it.id,
                    title: it.title,
                    foo: 'bar'
            ]
        }

        def book = bookClass.newInstance()
        book.id = 4711
        book.title = "The Definitive Guide to Grails"

        assertEquals( (book as JSON).toString(), """{"id":4711,"title":"The Definitive Guide to Grails","foo":"bar"}""" )

    }

    void testDefaultConverterConfigurationObjectMarshallerRegistration() {

        JSON.registerObjectMarshaller(java.sql.Date) {
            return it.toString()
        }

        JSON.registerObjectMarshaller(java.sql.Time) {
            return it.toString()
        }

        def objA = new java.sql.Date(System.currentTimeMillis())
        def objB = new java.sql.Time(System.currentTimeMillis())

        assertEquals( ([a:objA] as JSON).toString(), """{"a":"${objA}"}""")
        assertEquals( ([b:objB] as JSON).toString(), """{"b":"${objB}"}""")

    }

    void testMarshallerRegistrationOrder() {

        JSON.registerObjectMarshaller(Date) {
            return "FAIL"
        }

        JSON.registerObjectMarshaller(Date) {
            return "SUCCESS"
        }

        assertEquals( ([d: new Date()] as JSON).toString(), """{"d":"SUCCESS"}""")
    }

    void testMarshallerPriority() {

        def om1 = new org.codehaus.groovy.grails.web.converters.marshaller.ClosureOjectMarshaller(Date.class, { return "SUCCESS" })
        def om2 = new org.codehaus.groovy.grails.web.converters.marshaller.ClosureOjectMarshaller(Date.class, { return "FAIL" })

        JSON.registerObjectMarshaller(om1, 5)
        JSON.registerObjectMarshaller(om2, 3)

        assertEquals( ([d: new Date()] as JSON).toString(), """{"d":"SUCCESS"}""")
    }

    void testNamedConfigurations() {

        JSON.registerObjectMarshaller(Date) {
            "DEFAULT"
        }

        JSON.createNamedConfig("test-config") { DefaultConverterConfiguration cfg ->

            cfg.registerObjectMarshaller(Date) {
                return "TEST"
            }

        }

        def obj = [d: new Date()]
        assertEquals( (obj as JSON).toString(), """{"d":"DEFAULT"}""")

        JSON.use("test-config") {
            assertEquals( (obj as JSON).toString(), """{"d":"TEST"}""")
        }

        assertEquals( (obj as JSON).toString(), """{"d":"DEFAULT"}""")
        
    }

    protected void onSetUp() {

        gcl.parseClass """
            class Book {
               Long id
               Long version
               String title
               String author

            }
                      """
    }


}
