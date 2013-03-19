package org.codehaus.groovy.grails.scaffolding.view

import grails.util.GrailsWebUtil

import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.request.RequestContextHolder

class ScaffoldedGroovyPageViewTests extends GroovyTestCase {

    void testScaffoldedGroovyPageView() {
        def webRequest = GrailsWebUtil.bindMockWebRequest()

        def url = "/WEB-INF/grails-apps/views/test.gsp"
        def gpte = new GroovyPagesTemplateEngine(new MockServletContext())
        gpte.afterPropertiesSet()

        def ctx = new MockApplicationContext()
        ctx.registerMockBean(GroovyPagesTemplateEngine.BEAN_ID, gpte)

        def view = new ScaffoldedGroovyPageView(url, "<%='success'+foo%>")
        view.applicationContext = ctx
        view.templateEngine = gpte
        view.afterPropertiesSet()

        def model = [foo:"bar"]
        view.render(model, webRequest.currentRequest, webRequest.currentResponse)

        assertEquals "successbar", webRequest.currentResponse.contentAsString
    }

    protected void tearDown() {
        super.tearDown()
        RequestContextHolder.resetRequestAttributes()
    }
}
