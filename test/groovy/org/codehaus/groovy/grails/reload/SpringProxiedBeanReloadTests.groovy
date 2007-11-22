/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 22, 2007
 */
package org.codehaus.groovy.grails.reload

import grails.spring.BeanBuilder
import org.springframework.aop.framework.ProxyFactoryBean
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation

class SpringProxiedBeanReloadTests extends GroovyTestCase {

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

        println "Bean = " + appCtx.getBean("myBean")
        println "Class = " + appCtx.getBean('myBean').getClass()
        assertEquals "The Stand", appCtx.getBean('myBean').title
        assertEquals "Stephen King", appCtx.getBean('myBean').author


        gcl = new GroovyClassLoader()
        cls = gcl.parseClass("class Book { String title = 'The Shining'; String author }")

        bb = new BeanBuilder(gcl)
        def beanDefinitions = bb.beans {
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
    public Object invoke(MethodInvocation methodInvocation) {
        return methodInvocation.proceed();
    }

}