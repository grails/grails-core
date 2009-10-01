package org.codehaus.groovy.grails.scaffolding.view

import grails.util.*
import org.springframework.web.context.request.*
import org.springframework.mock.web.*
import org.codehaus.groovy.grails.web.pages.*
import org.codehaus.groovy.grails.support.*
import org.codehaus.groovy.grails.scaffolding.view.ScaffoldedGroovyPageView


class ScaffoldedGroovyPageViewTests extends GroovyTestCase {

    void testScaffoldedGroovyPageView() {
        def webRequest = GrailsWebUtil.bindMockWebRequest()


        def url = "/WEB-INF/grails-apps/views/test.gsp"
        def gpte = new GroovyPagesTemplateEngine(new MockServletContext())

        def ctx = new MockApplicationContext()
        ctx.registerMockBean(GroovyPagesTemplateEngine.BEAN_ID, gpte)

        def view = new ScaffoldedGroovyPageView(url, "<%='success'+foo%>")
        view.applicationContext = ctx
        view.templateEngine = gpte

        def model = [foo:"bar"]
        view.render(model, webRequest.currentRequest, webRequest.currentResponse)

        assertEquals "successbar", webRequest.currentResponse.contentAsString
    }

    void tearDown() {
         RequestContextHolder.setRequestAttributes(null)
    }
}