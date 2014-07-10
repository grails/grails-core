package org.grails.web.servlet.view;

import grails.util.*
import org.grails.core.io.MockStringResourceLoader
import org.grails.support.MockApplicationContext
import org.grails.web.pages.GroovyPagesTemplateEngine
import org.grails.web.servlet.view.GroovyPageView
import org.springframework.mock.web.*
import org.springframework.web.context.request.*

@SuppressWarnings("unused")
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
         RequestContextHolder.setRequestAttributes(null)
    }
}
