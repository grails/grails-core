package org.codehaus.groovy.grails.test

import junit.framework.TestCase
import grails.spring.WebBeanBuilder
import junit.framework.TestResult
import grails.spring.BeanBuilder
import javax.servlet.ServletContext
import org.springframework.mock.web.MockServletContext
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.springframework.web.context.request.RequestContextHolder

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jun 9, 2009
 */

public class GrailsTestSuiteTests extends GroovyTestCase{

    void testGrailsTestSuite() {
        BeanBuilder bb = new WebBeanBuilder()
        bb.springConfig.servletContext = new MockServletContext()
        bb.beans {
            foo(String, "bar")
        }

        def ctx = bb.createApplicationContext()

        def grailsTest = new GrailsTestSuite(ctx,"Tests")
        TestResult result = new TestResult()
        grailsTest.runTest(new GrailsTestSuiteTestCase(),result)

        assertEquals 1,result.failureCount()
        assertEquals 1,result.runCount()



        
    }
}

class GrailsTestSuiteTestCase extends TestCase {
    static transactional =false
    void testOne() {
        assertNotNull "ServletContextHolder should contain a servlet context",ServletContextHolder.servletContext
        assertNotNull "ApplicationHolder should contain an application",ApplicationHolder.application
        assertNotNull "RequestContextHolder should contain a request",RequestContextHolder.currentRequestAttributes()

    }
    void testTwo() { fail "bad" }
}