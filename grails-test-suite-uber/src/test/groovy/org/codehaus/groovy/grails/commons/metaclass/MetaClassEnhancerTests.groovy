
package org.codehaus.groovy.grails.commons.metaclass

import grails.util.GrailsWebUtil
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.MockGrailsPluginManager
import org.codehaus.groovy.grails.plugins.web.api.ControllersApi
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingsHolder
import org.springframework.web.context.request.RequestContextHolder

class MetaClassEnhancerTests extends GroovyTestCase {

    void testEnhanceMetaClass() {
        def ctx = new MockApplicationContext()

        def application = new DefaultGrailsApplication([TestController] as Class[], getClass().classLoader)
        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, application)

        ctx.registerMockBean "grailsUrlMappingsHolder", new DefaultUrlMappingsHolder([])

        def controllerApi = new ControllersApi(new MockGrailsPluginManager())

        def enhancer = new MetaClassEnhancer()
        enhancer.addApi controllerApi

        enhancer.enhance TestController.metaClass

        GrailsWebUtil.bindMockWebRequest()

        def controller = new TestController()

        controller.testRenderText()
        assert "hello world" == controller.response.contentAsString
    }

    @Override
    protected void tearDown() throws Exception {
        RequestContextHolder.setRequestAttributes(null)
    }
}
class TestController {

    def testRenderText = {
        render "hello world"
    }
}
