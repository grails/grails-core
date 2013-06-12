package org.grails.plugins.web.rest.render.xml

import grails.converters.XML
import grails.validation.ValidationErrors
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.web.converters.ConverterUtil
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationHolder
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.codehaus.groovy.grails.web.converters.marshaller.xml.ValidationErrorsMarshaller
import org.codehaus.groovy.grails.web.mime.MimeType
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.plugins.web.rest.render.ServletRenderContext
import org.grails.plugins.web.rest.render.html.Book
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class DefaultXmlRendererSpec extends Specification {

    void setup() {
        final initializer = new ConvertersConfigurationInitializer()
        initializer.initialize(new DefaultGrailsApplication())
        ValidationErrors.metaClass.asType = { Class type ->
            ConverterUtil.createConverter(type, delegate, null);
        }
        XML.registerObjectMarshaller(new ValidationErrorsMarshaller())
    }

    void cleanup() {
        GroovySystem.metaClassRegistry.removeMetaClass(ValidationErrors)
        ConvertersConfigurationHolder.clear()
    }

    void "Test that XML renderer writes XML to the response for a domain instance"() {
        when:"A domain instance is rendered"
            def renderer = new DefaultXmlRenderer(Book)
            final response = new MockHttpServletResponse()
            final webRequest = new GrailsWebRequest(new MockHttpServletRequest(), response, new MockServletContext())
            webRequest.actionName = "test"
            def renderContext = new ServletRenderContext(webRequest) {
                @Override
                MimeType getAcceptMimeType() {
                    MimeType.TEXT_XML
                }
            }
            final book = new Book(title: "The Stand")
            renderer.render(book,renderContext)


        then:"The model and view are populated correctly"
            response.contentType == 'text/xml'
            response.status == 200

        when:"The XML is parsed"
            def xml = new XmlSlurper().parseText(response.contentAsString)

        then:"It is correct"
            xml.title.text() == 'The Stand'
     }

    void "Test that XML renderer sets a model and view correctly for an Error instance"() {
        when:"A domain instance is rendered"
            def renderer = new DefaultXmlRenderer(Book)
            final response = new MockHttpServletResponse()
            final webRequest = new GrailsWebRequest(new MockHttpServletRequest(), response, new MockServletContext())
            webRequest.actionName = "test"
            def renderContext = new ServletRenderContext(webRequest) {
                @Override
                MimeType getAcceptMimeType() {
                    MimeType.TEXT_XML
                }
            }
            final book = new Book(title: "")
            final errors = new ValidationErrors(book)
            book.errors = errors
            errors.rejectValue("title", "title.blank.error")

            renderer.render(book.errors,renderContext)


        then:"The model and view are populated correctly"
            response.contentType == 'text/xml'
            response.status == 422

        when:"The XML is parsed"
            final text = response.contentAsString
            println text
            def xml = new XmlSlurper().parseText(text)

        then:"It is correct"
            xml.error.@field.text() == 'title'
    }
}
