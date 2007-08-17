package org.codehaus.groovy.grails.scaffolding

import grails.util.*
import org.springframework.web.context.request.*
import org.springframework.mock.web.*
import org.springframework.core.io.*
import org.springframework.web.context.request.*
import org.codehaus.groovy.grails.web.servlet.mvc.*
import org.codehaus.groovy.grails.web.errors.*
import org.codehaus.groovy.grails.web.pages.*
import org.codehaus.groovy.grails.support.*
import org.codehaus.groovy.grails.commons.*


class TemplateGeneratingResponseHandlerTests extends GroovyTestCase {

    def application
    
    void setUp() {
        application = new DefaultGrailsApplication([Test.class] as Class[], new GroovyClassLoader())
		application.initialise()
        System.setProperty("grails.env", "development")
    }
    void testCreateScaffoldedListResponse() {
        def webRequest = GrailsWebUtil.bindMockWebRequest()
        def ctx = new MockApplicationContext()
        def gpte  = new GroovyPagesTemplateEngine(webRequest.servletContext)
        ctx.registerMockBean(GroovyPagesTemplateEngine.BEAN_ID, gpte)

        webRequest.servletContext.setAttribute(ApplicationAttributes.APPLICATION_CONTEXT, ctx)
        webRequest.actionName = "list"

        def url = "/test/list"
        def handler = new TemplateGeneratingResponseHandler()
        handler.templateGenerator = new DefaultGrailsTemplateGenerator()
        handler.scaffoldedClass = Test.class
        handler.grailsApplication = application
        handler.applicationContext = ctx;

        def mv = handler.createScaffoldedResponse(url, [tests:[Test.newInstance()]], "list")

        assert mv
        assert mv.model.tests
        assert mv.view instanceof ScaffoldedGroovyPageView        
    }

    void testCreateScaffoldedShowResponse() {
        def webRequest = GrailsWebUtil.bindMockWebRequest()
        def ctx = new MockApplicationContext()
        def gpte  = new GroovyPagesTemplateEngine(webRequest.servletContext)
        ctx.registerMockBean(GroovyPagesTemplateEngine.BEAN_ID, gpte)

        webRequest.servletContext.setAttribute(ApplicationAttributes.APPLICATION_CONTEXT, ctx)

        webRequest.actionName = "show"

        def url = "/test/show"
        def handler = new TemplateGeneratingResponseHandler()
        handler.templateGenerator = new DefaultGrailsTemplateGenerator()
        handler.scaffoldedClass = Test.class
        handler.grailsApplication = application
        handler.applicationContext = ctx;

        def mv = handler.createScaffoldedResponse(url, [test:[Test.newInstance()]], "show")

        assert mv
        assert mv.model.test
        assert mv.view instanceof ScaffoldedGroovyPageView
    }

    void testCreateScaffoldedEditResponse() {
        def webRequest = GrailsWebUtil.bindMockWebRequest()
        def ctx = new MockApplicationContext()
        def gpte  = new GroovyPagesTemplateEngine(webRequest.servletContext)
        ctx.registerMockBean(GroovyPagesTemplateEngine.BEAN_ID, gpte)

        webRequest.servletContext.setAttribute(ApplicationAttributes.APPLICATION_CONTEXT, ctx)

        webRequest.actionName = "edit"

        def url = "/test/edit"
        def handler = new TemplateGeneratingResponseHandler()
        handler.templateGenerator = new DefaultGrailsTemplateGenerator()
        handler.scaffoldedClass = Test.class
        handler.grailsApplication = application
        handler.applicationContext = ctx;

        def mv = handler.createScaffoldedResponse(url, [test:[Test.newInstance()]], "edit")

        assert mv
        assert mv.model.test
        assert mv.view instanceof ScaffoldedGroovyPageView
    }

    void testCreateScaffoldedCreateResponse() {
        def webRequest = GrailsWebUtil.bindMockWebRequest()
        def ctx = new MockApplicationContext()
        def gpte  = new GroovyPagesTemplateEngine(webRequest.servletContext)
        ctx.registerMockBean(GroovyPagesTemplateEngine.BEAN_ID, gpte)

        webRequest.servletContext.setAttribute(ApplicationAttributes.APPLICATION_CONTEXT, ctx)

        webRequest.actionName = "create"

        def url = "/test/create"
        def handler = new TemplateGeneratingResponseHandler()
        handler.templateGenerator = new DefaultGrailsTemplateGenerator()
        handler.scaffoldedClass = Test.class
        handler.grailsApplication = application
        handler.applicationContext = ctx;

        def mv = handler.createScaffoldedResponse(url, [test:[Test.newInstance()]], "create")

        assert mv
        assert mv.model.test
        assert mv.view instanceof ScaffoldedGroovyPageView
    }

    void tearDown() {
         RequestContextHolder.setRequestAttributes(null)
         application = null
        System.setProperty("grails.env", "")         
    }
}
class Test {
    Long id
    Long version
    String name
}