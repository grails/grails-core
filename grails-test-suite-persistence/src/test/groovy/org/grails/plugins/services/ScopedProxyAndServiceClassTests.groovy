package org.grails.plugins.services

import grails.spring.WebBeanBuilder
import grails.util.GrailsWebMockUtil

import org.grails.commons.test.AbstractGrailsMockTests
import org.grails.spring.aop.autoproxy.GroovyAwareAspectJAwareAdvisorAutoProxyCreator
import org.springframework.aop.scope.ScopedProxyFactoryBean
import org.springframework.web.context.request.RequestContextHolder

class ScopedProxyAndServiceClassTests extends AbstractGrailsMockTests {

    // test for http://jira.codehaus.org/browse/GRAILS-6278
    void testScopedProxy() {
        def bb = new WebBeanBuilder()

        GrailsWebMockUtil.bindMockWebRequest()

        bb.beans {
            "org.springframework.aop.config.internalAutoProxyCreator"(GroovyAwareAspectJAwareAdvisorAutoProxyCreator)
            testService(TestService) { bean ->
                bean.scope = "session"

            }
            testScopeProxy(ScopedProxyFactoryBean) {
                targetBeanName="testService"
                proxyTargetClass=true
            }
        }

        def appCtx = bb.createApplicationContext()

        def testService = appCtx.getBean("testScopeProxy")

        assert testService != null
        assert "foo" == testService.myProperty
        assert "bar" == testService.serviceMethod()
        assert "bar" == testService.indirectServiceMethod()
    }

    protected void onTearDown() {
        RequestContextHolder.resetRequestAttributes()
    }
}

class TestService {

    def myProperty = "foo"

    def serviceMethod() {
        'bar'
    }

    def indirectServiceMethod() {
        serviceMethod()
    }

// - This was the workaround for the bug -
//
//    private MetaClass metaClass
//
//    TestService() {
//        this.metaClass = GroovySystem.metaClassRegistry.getMetaClass(TestService)
//    }
//
//    void setMetaClass(MetaClass mc) {
//        this.metaClass = mc
//    }
//
//    MetaClass getMetaClass() { metaClass }
}
