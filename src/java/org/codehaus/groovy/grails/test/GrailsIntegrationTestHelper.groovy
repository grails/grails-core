package org.codehaus.groovy.grails.test

import junit.framework.TestSuite
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext
import org.codehaus.groovy.grails.test.GrailsTestSuite
import org.codehaus.groovy.grails.test.DefaultGrailsTestHelper
import grails.util.BuildSettings

class GrailsIntegrationTestHelper extends DefaultGrailsTestHelper {
    GrailsWebApplicationContext applicationContext

    GrailsIntegrationTestHelper(
            BuildSettings settings,
            ClassLoader parentLoader,
            Closure resourceResolver,
            GrailsWebApplicationContext appContext) {
        super(settings, parentLoader, resourceResolver)
        this.applicationContext = appContext
    }

    TestSuite createTestSuite() {
        new GrailsTestSuite(this.applicationContext, this.testSuffix)
    }

    TestSuite createTestSuite(Class clazz) {
        new GrailsTestSuite(this.applicationContext, clazz, this.testSuffix)
    }
}
