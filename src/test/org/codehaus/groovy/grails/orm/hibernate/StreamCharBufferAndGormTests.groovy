package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.web.util.StreamCharBuffer
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import grails.util.GrailsWebUtil
import org.springframework.web.context.request.RequestContextHolder

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class StreamCharBufferAndGormTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class StreamCharMe {
    String name
}
''')
        gcl.parseClass('''
class StreamCharTagLib {
    def callMe = { attrs, body ->
        out << "hello"
    }
}
''')


    }



    void testGormWithStreamCharBuffer() {
        GrailsWebUtil.bindMockWebRequest(appCtx)

        try {
            def StreamCharMe = ga.getDomainClass('StreamCharMe').clazz

            assert StreamCharMe.newInstance(name:"hello").save(flush:true) : "should have saved instance"
            session.clear()

            def taglib = appCtx.getBean("StreamCharTagLib")
            def result = taglib.callMe()
            assert result instanceof StreamCharBuffer : "should be a stream char buffer!"

            assert StreamCharMe.findByName(result) : "should have found a result when passing a StreamCharBuffer value"
            assert StreamCharMe.countByName(result) : "should have found a result when passing a StreamCharBuffer value"
            assert StreamCharMe.findAllByName(result) : "should have found a result when passing a StreamCharBuffer value"
            assert StreamCharMe.findWhere(name:result) : "should have found a result when passing a StreamCharBuffer value"
            assert StreamCharMe.findAllWhere(name:result) : "should have found a result when passing a StreamCharBuffer value"
            assert StreamCharMe.withCriteria{ eq 'name',result } : "should have found a result when passing a StreamCharBuffer value"
            assert StreamCharMe.find("from StreamCharMe s where s.name = ?", [result] ) : "should have found a result when passing a StreamCharBuffer value"
            assert StreamCharMe.findAll("from StreamCharMe s where s.name = ?", [result] ) : "should have found a result when passing a StreamCharBuffer value"
            assert StreamCharMe.executeQuery("from StreamCharMe s where s.name = ?", [result] ) : "should have found a result when passing a StreamCharBuffer value"

        }
        finally {
            RequestContextHolder.setRequestAttributes(null)
        }
    }
}