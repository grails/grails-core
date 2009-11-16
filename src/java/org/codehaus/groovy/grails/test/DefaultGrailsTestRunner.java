package org.codehaus.groovy.grails.test;

import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.codehaus.groovy.grails.test.io.SystemOutAndErrSwapper;

import org.codehaus.groovy.grails.test.report.junit.JUnitReports;
import org.codehaus.groovy.grails.test.report.junit.JUnitReportsFactory;

/**
 * <p>Runs a JUnit test suite, printing the results to the console and
 * also generating reports in selected formats.</p>
 * <p><b>Note</b> This class is currently written in Java because the
 * Groovy compiler can't cope with "ant-junit.jar" in ANT_HOME and
 * "junit.jar" in the "lib" directory.</p>
 *
 * @author Peter Ledbrook
 */
public class DefaultGrailsTestRunner implements GrailsTestRunner {
    private SystemOutAndErrSwapper outAndErrSwapper = new SystemOutAndErrSwapper();
    private JUnitReportsFactory reportsFactory;
    
    public DefaultGrailsTestRunner(JUnitReportsFactory reportsFactory) {
        this.reportsFactory = reportsFactory;
    }

    public TestResult runTests(TestSuite suite) {
        TestResult result = new TestResult();
        
        GrailsConsoleResultFormatter consoleFormatter = new GrailsConsoleResultFormatter();
        consoleFormatter.setOutput(System.out);
        result.addListener(consoleFormatter);

        for (Enumeration tests = suite.tests(); tests.hasMoreElements();) {
            TestSuite test = (TestSuite) tests.nextElement();

            JUnitTest junitTest = new JUnitTest(test.getName());
            JUnitReports reports = (JUnitReports)reportsFactory.createReports(test.getName());
            
            try {
                
                result.addListener(reports);
                
                consoleFormatter.startTestSuite(junitTest);
                reports.startTestSuite(junitTest);
                
                // Starting...now!
                long start = System.currentTimeMillis();
                int runCount = result.runCount();
                int failureCount = result.failureCount();
                int errorCount = result.errorCount();

                outAndErrSwapper.swapIn();
                
                for (int i = 0; i < test.testCount(); i++) {
                    TestCase t = (TestCase) test.testAt(i);
                    System.out.println("--Output from " + t.getName() + "--");
                    System.err.println("--Output from " + t.getName() + "--");

                    test.runTest(t, result);
                }

                junitTest.setCounts(
                        result.runCount() - runCount,
                        result.failureCount() - failureCount,
                        result.errorCount() - errorCount);
                junitTest.setRunTime(System.currentTimeMillis() - start);
            }
            finally {
                List<OutputStream> outAndErr = outAndErrSwapper.swapOut();

                reports.setSystemOutput(outAndErr.get(0).toString());
                reports.setSystemError(outAndErr.get(1).toString());
                reports.endTestSuite(junitTest);
                
                consoleFormatter.endTestSuite(junitTest);
                
            }
            
            result.removeListener(reports);
        }

        return result;
    }

}
