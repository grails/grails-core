package org.grails.plugins.web.rest.render.xml

import groovy.transform.CompileStatic
import groovy.xml.XmlSlurper
import grails.converters.XML
import grails.core.DefaultGrailsApplication
import grails.persistence.Entity
import grails.testing.gorm.DomainUnitTest
import grails.util.GrailsWebUtil
import grails.validation.ValidationErrors
import grails.web.mime.MimeType
import groovy.xml.slurpersupport.GPathResult
import org.grails.plugins.web.rest.render.ServletRenderContext
import org.grails.web.converters.configuration.ConvertersConfigurationHolder
import org.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.grails.web.converters.marshaller.xml.ValidationErrorsMarshaller
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext

import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class DefaultXmlRendererSpec extends Specification implements DomainUnitTest<XmlBook> {

    void setup() {
        final initializer = new ConvertersConfigurationInitializer()
        initializer.grailsApplication = new DefaultGrailsApplication()
        initializer.initialize()
        XML.registerObjectMarshaller(new ValidationErrorsMarshaller())
    }

    void cleanup() {
        GroovySystem.metaClassRegistry.removeMetaClass(ValidationErrors)
        ConvertersConfigurationHolder.clear()
    }

    void 'Test that XML renderer writes XML to the response for a domain instance'() {
        when: 'A domain instance is rendered'
            def renderer = new DefaultXmlRenderer(XmlBook)
            def response = new MockHttpServletResponse()
            def webRequest = new GrailsWebRequest(new MockHttpServletRequest(), response, new MockServletContext())
            webRequest.actionName = 'test'
            def renderContext = new ServletRenderContext(webRequest) {
                @Override
                MimeType getAcceptMimeType() {
                    MimeType.TEXT_XML
                }
            }
            def book = new XmlBook(title: 'The Stand')
            renderer.render(book, renderContext)

        then: 'The model and view are populated correctly'
            response.contentType == GrailsWebUtil.getContentType('text/xml', GrailsWebUtil.DEFAULT_ENCODING)
            response.status == 200

        when: 'The XML is parsed'
            def xml = new XmlSlurper().parseText(response.contentAsString)

        then: 'It is correct'
        (xml['title'] as GPathResult).text() == 'The Stand'
     }

    void 'Test that XML renderer sets a model and view correctly for an Error instance'() {
        when: 'A domain instance is rendered'
            def renderer = new DefaultXmlRenderer(XmlBook)
            def response = new MockHttpServletResponse()
            def webRequest = new GrailsWebRequest(new MockHttpServletRequest(), response, new MockServletContext())
            webRequest.actionName = 'test'
            def renderContext = new ServletRenderContext(webRequest) {
                @Override
                MimeType getAcceptMimeType() {
                    MimeType.TEXT_XML
                }
            }
            def book = new XmlBook(title: '')
            def errors = new ValidationErrors(book)
            book.errors = errors
            errors.rejectValue('title', 'title.blank.error')
            renderer.render(book.errors,renderContext)

        then: 'The model and view are populated correctly'
            response.contentType == GrailsWebUtil.getContentType('text/xml', GrailsWebUtil.DEFAULT_ENCODING)
            response.status == 422

        when: 'The XML is parsed'
            def text = response.contentAsString
            def xml = new XmlSlurper().parseText(text)

        then: 'It is correct'
            (xml['error']['@field'] as GPathResult).text() == 'title'
    }
}

@Entity
class XmlBook {

    String title

    @SuppressWarnings('unused')
    static constraints = {
        title(blank: false)
    }
}
