package org.codehaus.groovy.grails.commons.metaclass

import grails.util.GrailsWebUtil

import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import org.grails.plugins.MockGrailsPluginManager
import org.grails.plugins.web.controllers.api.ControllersApi
import org.grails.support.MockApplicationContext
import org.grails.core.metaclass.MetaClassEnhancer
import org.grails.web.mapping.DefaultUrlMappingsHolder
import org.springframework.web.context.request.RequestContextHolder

class MetaClassEnhancerTests extends GroovyTestCase {

    void testEnhanceMetaClass() {
        def ctx = new MockApplicationContext()

        def application = new DefaultGrailsApplication([TestMetaClassController] as Class[], getClass().classLoader)
        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, application)

        ctx.registerMockBean "grailsUrlMappingsHolder", new DefaultUrlMappingsHolder([])

        def controllerApi = new ControllersApi(new MockGrailsPluginManager())

        def enhancer = new MetaClassEnhancer()
        enhancer.addApi controllerApi

        enhancer.enhance TestMetaClassController.metaClass

        GrailsWebUtil.bindMockWebRequest()

        def controller = new TestMetaClassController()

        controller.testRenderText()
        assert "hello world" == controller.response.contentAsString
    }

    @Override
    protected void tearDown() throws Exception {
        RequestContextHolder.setRequestAttributes(null)
    }
}

class TestMetaClassController {

    def testRenderText = {
        render "hello world"
    }
}
