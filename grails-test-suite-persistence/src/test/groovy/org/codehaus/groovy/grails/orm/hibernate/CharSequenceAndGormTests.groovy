package org.codehaus.groovy.grails.orm.hibernate

import grails.util.GrailsWebUtil

import org.codehaus.groovy.grails.web.util.StreamCharBuffer
import org.springframework.web.context.request.RequestContextHolder

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class CharSequenceAndGormTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class SomeDomainClass {
    String name
}
'''

        gcl.parseClass '''
class StreamCharTagLib {
    Closure callMe = { attrs, body ->
        out << "hello"
    }
}
'''
    }

    void testGormWithStreamCharBuffer() {
        GrailsWebUtil.bindMockWebRequest(appCtx)

        try {
            def someDomainClass = ga.getDomainClass('SomeDomainClass').clazz

            assertNotNull "should have saved instance", someDomainClass.newInstance(name:"hello").save(flush:true)
            session.clear()

            def taglib = appCtx.getBean("StreamCharTagLib")
            def result = taglib.callMe()
            assertTrue result instanceof StreamCharBuffer

            testQueryMethods result
        }
        finally {
            RequestContextHolder.setRequestAttributes(null)
        }
    }

    void testGormWithGString() {
        GrailsWebUtil.bindMockWebRequest(appCtx)

        try {
            def someDomainClass = ga.getDomainClass('SomeDomainClass').clazz

            assertNotNull "should have saved instance", someDomainClass.newInstance(name:"hello").save(flush:true)
            session.clear()

            def value = 'hello'
            def queryArg = "${value}"
            assertTrue queryArg instanceof GString
            testQueryMethods queryArg
        }
        finally {
            RequestContextHolder.setRequestAttributes(null)
        }
    }

    private testQueryMethods(queryArg) {
        def someDomainClass = ga.getDomainClass('SomeDomainClass').clazz
        assert someDomainClass.findByName(queryArg) : "should have found a result when passing a ${queryArg.getClass()} value"
        assert someDomainClass.findByNameLike(queryArg) : "should have found a result when passing a ${queryArg.getClass()} value"
        assert someDomainClass.countByName(queryArg) : "should have found a result when passing a ${queryArg.getClass()} value"
        assert someDomainClass.countByNameLike(queryArg) : "should have found a result when passing a ${queryArg.getClass()} value"
        assert someDomainClass.findAllByName(queryArg) : "should have found a result when passing a ${queryArg.getClass()} value"
        assert someDomainClass.findAllByNameLike(queryArg) : "should have found a result when passing a ${queryArg.getClass()} value"
        assert someDomainClass.findWhere(name:queryArg) : "should have found a result when passing a ${queryArg.getClass()} value"
        assert someDomainClass.findAllWhere(name:queryArg) : "should have found a result when passing a ${queryArg.getClass()} value"
        assert someDomainClass.withCriteria{ eq 'name',queryArg } : "should have found a result when passing a ${queryArg.getClass()} value"
        assert someDomainClass.find("from SomeDomainClass s where s.name = ?", [queryArg]) : "should have found a result when passing a ${queryArg.getClass()} value"
        assert someDomainClass.findAll("from SomeDomainClass s where s.name = ?", [queryArg]) : "should have found a result when passing a ${queryArg.getClass()} value"
        assert someDomainClass.executeQuery("from SomeDomainClass s where s.name = ?", [queryArg]) : "should have found a result when passing a ${queryArg.getClass()} value"
    }
}
