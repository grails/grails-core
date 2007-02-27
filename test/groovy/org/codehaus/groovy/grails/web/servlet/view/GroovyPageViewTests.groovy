package org.codehaus.groovy.grails.web.servlet.view;

import grails.util.*
import org.springframework.web.context.request.*
import org.springframework.mock.web.*
import org.springframework.core.io.*
import org.springframework.web.context.request.*
import org.codehaus.groovy.grails.web.servlet.mvc.*
import org.codehaus.groovy.grails.web.errors.*
import org.codehaus.groovy.grails.web.pages.*
import org.codehaus.groovy.grails.support.*


class GroovyPageViewTests extends GroovyTestCase {

    void testGroovyPageView() {
        def webRequest = GrailsWebUtil.bindMockWebRequest()
        
        def rl = new MockStringResourceLoader()

        def url = "/WEB-INF/grails-apps/views/test.gsp"

        rl.registerMockResource(url, "<%='success'+foo%>")


        def gpte = new GroovyPagesTemplateEngine(new MockServletContext(rl))

        def view = new GroovyPageView()
        view.url = url
        view.templateEngine = gpte

        def model = [foo:"bar"]
        view.render(model, webRequest.currentRequest, webRequest.currentResponse)

        assertEquals "successbar", webRequest.currentResponse.delegate.contentAsString
    }

    void tearDown() {
         RequestContextHolder.setRequestAttributes(null)
    }
}