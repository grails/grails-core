package org.codehaus.groovy.grails.test;

import junit.framework.TestResult;
import junit.framework.TestSuite;

public interface GrailsTestRunner {
    TestResult runTests(TestSuite suite);
}
