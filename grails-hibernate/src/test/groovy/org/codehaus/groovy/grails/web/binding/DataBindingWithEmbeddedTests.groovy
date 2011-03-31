package org.codehaus.groovy.grails.web.binding

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.springframework.mock.web.MockHttpServletRequest
import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

/**
 * @author Rob Fletcher
 * @since 1.3.3
 */
class DataBindingWithEmbeddedTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass("""
            import grails.persistence.*

            @Entity
            class Reader {
                String name
                Book currentlyReading
            }

            @Entity
            class Book {
                Author author
                String title
                static constraints = {
                }
                static embedded = ["author"]
            }

            class Author {
                String name
                static constraints = {
                    name blank: false
                }
            }
        """)
    }


    void testDataBindingWithEmbeddedProperty() {
        def Book = ga.getDomainClass("Book").clazz

        def request = new MockHttpServletRequest()
        request.addParameter("title", "Pattern Recognition")
        request.addParameter("author.name", "William Gibson")
        def params = new GrailsParameterMap(request)

        def book = Book.newInstance()

        assertThat "Embedded property before binding", book.author, nullValue()

        book.properties = params

        assertThat "Embedded property after binding", book.author, notNullValue()
        assertThat "Embedded property after binding", book.author.name, equalTo("William Gibson")
    }

    void testDataBindingWithEmbeddedPropertyOfAssociation() {
        def Reader = ga.getDomainClass("Reader").clazz

        def request = new MockHttpServletRequest()
        request.addParameter("currentlyReading.title", "Pattern Recognition")
        request.addParameter("currentlyReading.author.name", "William Gibson")
        def params = new GrailsParameterMap(request)

        def reader = Reader.newInstance()
        reader.properties = params

        assertThat "Regular association property", reader.currentlyReading.title, equalTo("Pattern Recognition")
        assertThat "Embedded association property", reader.currentlyReading.author, notNullValue()
        assertThat "Embedded association property", reader.currentlyReading.author.name, equalTo("William Gibson")
    }
}
