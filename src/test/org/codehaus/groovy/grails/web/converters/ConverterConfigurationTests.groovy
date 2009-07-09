package org.codehaus.groovy.grails.web.converters

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import grails.converters.JSON

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
