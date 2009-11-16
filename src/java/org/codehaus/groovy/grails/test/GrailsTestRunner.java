package org.codehaus.groovy.grails.test;

import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.codehaus.groovy.grails.test.event.GrailsTestEventPublisher;

public interface GrailsTestRunner {
    TestResult runTests(TestSuite suite, GrailsTestEventPublisher eventPublisher);
}
