package org.codehaus.groovy.grails.plugins.services

import org.codehaus.groovy.grails.commons.test.AbstractGrailsMockTests
import grails.spring.BeanBuilder
import org.springframework.web.context.support.WebApplicationContextUtils
import grails.util.GrailsWebUtil
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.aop.framework.autoproxy.GroovyAwareAspectJAwareAdvisorAutoProxyCreator

/**
 */
class ScopedProxyAndServiceClassTests extends AbstractGrailsMockTests {


    // test for http://jira.codehaus.org/browse/GRAILS-6278
    void testScopedProxy() {
        if(notYetImplemented()) return
        
        def bb = new BeanBuilder()

        GrailsWebUtil.bindMockWebRequest()

        bb.beans {
            WebApplicationContextUtils.registerWebApplicationScopes(springConfig.beanFactory);
            "org.springframework.aop.config.internalAutoProxyCreator"(GroovyAwareAspectJAwareAdvisorAutoProxyCreator)
            testService(TestService) { bean ->
                bean.scope = "session"

            }
        	testScopeProxy(org.springframework.aop.scope.ScopedProxyFactoryBean){
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

        RequestContextHolder.setRequestAttributes null
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
