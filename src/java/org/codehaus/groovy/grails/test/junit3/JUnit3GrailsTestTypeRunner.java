/*
 * Copyright 2009 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package org.codehaus.groovy.grails.test.junit3;

import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.codehaus.groovy.grails.test.event.GrailsTestEventPublisher;
import org.codehaus.groovy.grails.test.io.SystemOutAndErrSwapper;
import org.codehaus.groovy.grails.test.report.junit.JUnitReports;
import org.codehaus.groovy.grails.test.report.junit.JUnitReportsFactory;

import java.io.OutputStream;
import java.util.Enumeration;
import java.util.List;

public class JUnit3GrailsTestTypeRunner {
    
    private SystemOutAndErrSwapper outAndErrSwapper;
    private JUnitReportsFactory reportsFactory;
    private GrailsTestEventPublisher eventPublisher;
    
    public JUnit3GrailsTestTypeRunner(JUnitReportsFactory reportsFactory, GrailsTestEventPublisher eventPublisher, SystemOutAndErrSwapper outAndErrSwapper) {
        this.reportsFactory = reportsFactory;
        this.eventPublisher = eventPublisher;
        this.outAndErrSwapper = outAndErrSwapper;
    }

    public TestResult runTests(TestSuite suite) {
        TestResult result = new TestResult();
        
        JUnit3ListenerEventPublisherAdapter eventPublisherAdapter = new JUnit3ListenerEventPublisherAdapter(eventPublisher);
        result.addListener(eventPublisherAdapter);
                
        for (Enumeration tests = suite.tests(); tests.hasMoreElements();) {
            TestSuite test = (TestSuite) tests.nextElement();

            JUnitTest junitTest = new JUnitTest(test.getName());
            JUnitReports reports = reportsFactory.createReports(test.getName());
            
            try {
                outAndErrSwapper.swapIn();
                
                result.addListener(reports);
                
                reports.startTestSuite(junitTest);
                eventPublisherAdapter.startTestSuite(junitTest);
                
                // Starting...now!
                long start = System.currentTimeMillis();
                int runCount = result.runCount();
                int failureCount = result.failureCount();
                int errorCount = result.errorCount();

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
                String out = outAndErr.get(0).toString();
                String err = outAndErr.get(1).toString();
                
                reports.setSystemOutput(out);
                reports.setSystemError(err);
                reports.endTestSuite(junitTest);
                
                eventPublisherAdapter.endTestSuite(junitTest, out, err);
            }
            
            result.removeListener(reports);
        }

        return result;
    }
}
