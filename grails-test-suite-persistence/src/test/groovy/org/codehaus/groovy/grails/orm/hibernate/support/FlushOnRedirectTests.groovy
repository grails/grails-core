package org.codehaus.groovy.grails.orm.hibernate.support

import grails.util.GrailsWebUtil

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest

import org.hibernate.FlushMode

import org.springframework.web.context.request.RequestContextHolder

/**
 * @author Graeme Rocher
 * @since 1.2
 */
class FlushOnRedirectTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class RedirectFlushTestDomain {
   String name
}
'''
        gcl.parseClass '''
class FlushOnRedirectController {
    def test = {
        def f = RedirectFlushTestDomain.get(1)
        f.name = "changed"
        redirect(action:"two")
    }

    def two = {}
}
'''
    }

    void testFlushOnRedirect() {
        def FlushOnRedirect = ga.getDomainClass("RedirectFlushTestDomain").clazz

        assertNotNull "should have saved instance", FlushOnRedirect.newInstance(name:"Bob").save(flush:true)

        session.clear()

        def controllerClass = ga.getControllerClass("FlushOnRedirectController").clazz

        def c = controllerClass.newInstance()
        GrailsWebUtil.bindMockWebRequest(appCtx)
        RequestContextHolder.requestAttributes.controllerName = "flushOnRedirect"

        c.test()
        session.clear()

        def t = FlushOnRedirect.get(1)

        assertEquals "Should have changed name of during flush", t.name, "changed"
    }

    void testNoFlushOnFlushModeManual() {
        def FlushOnRedirect = ga.getDomainClass("RedirectFlushTestDomain").clazz

        assertNotNull "should have saved instance", FlushOnRedirect.newInstance(name:"Bob").save(flush:true)

        session.clear()
        session.setFlushMode(FlushMode.MANUAL)
        def controllerClass = ga.getControllerClass("FlushOnRedirectController").clazz

        def c = controllerClass.newInstance()
        GrailsWebUtil.bindMockWebRequest(appCtx)
        RequestContextHolder.requestAttributes.controllerName = "flushOnRedirect"

        c.test()
        session.clear()

        def t = FlushOnRedirect.get(1)
        assertEquals "Should have changed name of during flush", t.name, "Bob"
    }

    protected void onTearDown() {
        RequestContextHolder.setRequestAttributes(null)
    }
}
