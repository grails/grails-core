package org.grails.web.servlet.view

import grails.util.GrailsWebMockUtil
import org.grails.core.io.MockStringResourceLoader
import org.grails.gsp.GroovyPagesTemplateEngine
import org.grails.support.MockApplicationContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.web.context.request.RequestContextHolder

import static org.junit.jupiter.api.Assertions.assertEquals

@SuppressWarnings("unused")
@Disabled("grails-gsp is not on jakarta.servlet yet")
class GroovyPageViewTests {

    @Test
    void testGroovyPageView() {
        def webRequest = GrailsWebMockUtil.bindMockWebRequest()

        def rl = new MockStringResourceLoader()

        def url = "/WEB-INF/grails-app/views/test.gsp"

        rl.registerMockResource(url, "<%='success'+foo%>")

        def gpte = new GroovyPagesTemplateEngine()
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

    @AfterEach
    void tearDown() {
         RequestContextHolder.resetRequestAttributes()
    }
}
