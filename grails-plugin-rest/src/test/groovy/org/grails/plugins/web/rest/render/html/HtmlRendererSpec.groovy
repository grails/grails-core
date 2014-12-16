package org.grails.plugins.web.rest.render.html

import grails.persistence.Entity
import grails.validation.ValidationErrors
import grails.web.mime.MimeType
import org.grails.web.util.GrailsApplicationAttributes
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.plugins.web.rest.render.ServletRenderContext
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import org.springframework.web.servlet.ModelAndView
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */

class HtmlRendererSpec extends Specification {


    void "Test that HTML renderer sets a model and view correctly for a domain instance"() {
        when:"A domain instance is rendered"
            def renderer = new DefaultHtmlRenderer(Book)
            final webRequest = new GrailsWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockServletContext())
            webRequest.actionName = "test"
            def renderContext = new ServletRenderContext(webRequest) {
                @Override
                MimeType getAcceptMimeType() {
                    MimeType.HTML
                }
            }
            final book = new Book(title: "The Stand")
            renderer.render(book,renderContext)

            ModelAndView modelAndView = webRequest.currentRequest.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)
        then:"The model and view are populated correctly"
            modelAndView
            modelAndView.viewName == 'test'
            modelAndView.model == [book:book]
    }

    void "Test that HTML renderer sets a model and view correctly for a domain instance and custom view and model"() {
        when:"A domain instance is rendered"
            def renderer = new DefaultHtmlRenderer(Book)
            final webRequest = new GrailsWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockServletContext())
            webRequest.actionName = "test"
            def renderContext = new ServletRenderContext(webRequest, [model:[foo:'bar'],view:"foo"]) {
                @Override
                MimeType getAcceptMimeType() {
                    MimeType.HTML
                }
            }
            final book = new Book(title: "The Stand")
            renderer.render(book,renderContext)

            ModelAndView modelAndView = webRequest.currentRequest.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)
        then:"The model and view are populated correctly"
            modelAndView
            modelAndView.viewName == 'foo'
            modelAndView.model == [foo: "bar", book:book]
    }

    void "Test that HTML renderer sets the correct model for a list of domains"() {
        when:"A domain instance is rendered"
            def renderer = new DefaultHtmlRenderer(Book)
            final webRequest = new GrailsWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockServletContext())
            webRequest.actionName = "test"
            def renderContext = new ServletRenderContext(webRequest){
                @Override
                MimeType getAcceptMimeType() {
                    MimeType.HTML
                }
            }
            final books = [new Book(title: "The Stand")]
            renderer.render(books,renderContext)

            ModelAndView modelAndView = webRequest.currentRequest.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)
        then:"The model and view are populated correctly"
            modelAndView
            modelAndView.viewName == 'test'
            modelAndView.model == [bookList:books]
    }

    void "Test that HTML renderer sets the correct model for an error"() {
        when:"A domain instance is rendered"
            def renderer = new DefaultHtmlRenderer(Book)
            final webRequest = new GrailsWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockServletContext())
            webRequest.actionName = "test"
            def renderContext = new ServletRenderContext(webRequest){
                @Override
                MimeType getAcceptMimeType() {
                    MimeType.HTML
                }
            }
            final book = new Book(title: "The Stand")
            final errors = new ValidationErrors(book)
            book.errors = errors
            errors.rejectValue("title", "title.blank.error")

            renderer.render(book.errors,renderContext)

        ModelAndView modelAndView = webRequest.currentRequest.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)
            then:"The model and view are populated correctly"
            modelAndView
            modelAndView.viewName == 'test'
            modelAndView.model == [book:book]
        }
}
@Entity
class Book {
    String title
    static constraints = {
        title blank:false
    }
}
