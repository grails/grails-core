package org.grails.reload

import grails.spring.BeanBuilder
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import org.junit.Ignore
import org.junit.jupiter.api.Test
import org.springframework.aop.framework.ProxyFactoryBean

import static org.junit.jupiter.api.Assertions.assertEquals

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@Ignore //TODO Ignore for JDK 11
class SpringProxiedBeanReloadTests {

    @Test
    void testReloadCGLibProxiedBean() {
        def gcl = new GroovyClassLoader()
        def cls = gcl.parseClass("class Book { String title = 'The Stand'; String author }")

        def bb = new BeanBuilder(gcl)
        bb.beans {
            interceptor(DummyInterceptor)
            target(cls) {
               author = "Stephen King"
            }
            myBean(ProxyFactoryBean) {
                targetName = 'target'
                autodetectInterfaces = false
                interceptorNames = 'interceptor'
            }
        }

        def appCtx = bb.createApplicationContext()

        assertEquals "The Stand", appCtx.getBean('myBean').title
        assertEquals "Stephen King", appCtx.getBean('myBean').author

        gcl = new GroovyClassLoader()
        cls = gcl.parseClass("class Book { String title = 'The Shining'; String author }")

        bb = new BeanBuilder(gcl)
        bb.beans {
            interceptor(DummyInterceptor)
            target(cls) {
               author = "Stephen King"
            }
            myBean(ProxyFactoryBean) {
                targetName = 'target'
                autodetectInterfaces = false
                interceptorNames = 'interceptor'
            }
        }

        bb.registerBeans(appCtx)

        assertEquals "The Shining", appCtx.getBean('myBean').title
        assertEquals "Stephen King", appCtx.getBean('myBean').author
    }

}
class DummyInterceptor implements MethodInterceptor {
    Object invoke(MethodInvocation methodInvocation) {
        methodInvocation.proceed()
    }
}
