package org.codehaus.groovy.grails.test;

import junit.framework.TestSuite;
import java.util.List;

public interface GrailsTestHelper {
    TestSuite createTests(List<String> testNames, String type);
}
