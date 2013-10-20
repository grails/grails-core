package org.codehaus.groovy.grails.web.servlet.view

import grails.util.GrailsWebUtil

import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.support.MockStringResourceLoader
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.request.RequestContextHolder

class GroovyPageViewTests extends GroovyTestCase {

    void testGroovyPageView() {
        def webRequest = GrailsWebUtil.bindMockWebRequest()

        def rl = new MockStringResourceLoader()

        def url = "/WEB-INF/grails-app/views/test.gsp"

        rl.registerMockResource(url, "<%='success'+foo%>")

        def gpte = new GroovyPagesTemplateEngine(new MockServletContext(rl))
        gpte.afterPropertiesSet()

        gpte.groovyPageLocator.addResourceLoader(rl)

        def ctx = new MockApplicationContext()
        ctx.registerMockBean(GroovyPagesTemplateEngine.BEAN_ID, gpte)

        def view = new GroovyPageView()
        view.url = url
        view.applicationContext = ctx
        view.templateEngine = gpte
        view.afterPropertiesSet()

        def model = [foo:"bar"]
        view.render(model, webRequest.currentRequest, webRequest.currentResponse)

        assertEquals "successbar", webRequest.currentResponse.contentAsString
    }

    void tearDown() {
        super.tearDown()
        RequestContextHolder.setRequestAttributes(null)
    }
}
