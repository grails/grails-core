package org.codehaus.groovy.grails.orm.hibernate.support

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import grails.util.GrailsWebUtil
import org.springframework.web.context.request.RequestContextHolder

/**
 * @author Graeme Rocher
 * @since 1.2
 */

public class FlushOnRedirectTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        super.onSetUp();
        gcl.parseClass('''
import grails.persistence.*

@Entity
class RedirectFlushTestDomain {
   String name
}     ''')

       gcl.parseClass('''
class FlushOnRedirectController {
    def test = {
        def f = RedirectFlushTestDomain.get(1)
        f.name = "changed"
        redirect(action:"two")
    }

    def two = {}
}
''')
    }


    void testFlushOnRedirect() {
        def FlushOnRedirect = ga.getDomainClass("RedirectFlushTestDomain").clazz

        assert FlushOnRedirect.newInstance(name:"Bob").save(flush:true) : "should have saved instance"

        session.clear()

        def controllerClass = ga.getControllerClass("FlushOnRedirectController").clazz

        def c = controllerClass.newInstance()

        GrailsWebUtil.bindMockWebRequest(appCtx)
        c.test()
        session.clear()


        def t = FlushOnRedirect.get(1)

        assertEquals "Should have changed name of during flush", t.name, "changed"
    }

    protected void onTearDown() {
        super.onTearDown();

        RequestContextHolder.setRequestAttributes(null)
    }


}