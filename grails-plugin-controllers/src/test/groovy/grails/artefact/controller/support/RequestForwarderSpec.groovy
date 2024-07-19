package grails.artefact.controller.support

import grails.util.GrailsWebMockUtil
import grails.web.mapping.LinkGenerator
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.servlet.mvc.ParameterCreationListener
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockRequestDispatcher
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.ModelAndView
import spock.lang.Specification

import jakarta.servlet.RequestDispatcher
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse

/**
 * Created by graemerocher on 21/02/2017.
 */
class RequestForwarderSpec extends Specification {

    void "test request forward cleans up request attributes after forward"() {

        setup:
        def applicationContext = Mock(WebApplicationContext)
        def linkGenerator = Mock(LinkGenerator)
        linkGenerator.link(_) >> "/test"
        applicationContext.getBean(LinkGenerator) >> linkGenerator
        applicationContext.getBeansOfType(ParameterCreationListener) >> [:]
        MockRequestDispatcher mockRequestDispatcher = new MockRequestDispatcher("test") {
            @Override
            void forward(ServletRequest request, ServletResponse response) {
                request.setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, new ModelAndView())
                super.forward(request, response)
            }
        };

        GrailsWebRequest webRequest = GrailsWebMockUtil.bindMockWebRequest(applicationContext, new MockHttpServletRequest() {
            @Override
            RequestDispatcher getRequestDispatcher(String path) {
                return mockRequestDispatcher
            }
        }, new MockHttpServletResponse())

        when:"A forward is issued that populates the model"
        TestForwarder forwarder = new TestForwarder()

        forwarder.doForward()

        then:"The model and view attribute is cleared"
        webRequest.request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW) == null



        cleanup:
        RequestContextHolder.setRequestAttributes(null)
    }
}

class TestForwarder implements RequestForwarder {
    void doForward() {
        forward(controller:"blah")
    }
}
