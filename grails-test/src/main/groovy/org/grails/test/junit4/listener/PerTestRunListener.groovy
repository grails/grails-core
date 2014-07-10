/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.test.junit4.listener

import groovy.transform.CompileStatic
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest
import org.grails.test.report.junit.JUnitReports
import org.grails.test.event.GrailsTestEventPublisher
import org.grails.test.io.SystemOutAndErrSwapper
import org.junit.runner.Description
import org.junit.runner.notification.Failure

import junit.framework.JUnit4TestCaseFacade
import junit.framework.AssertionFailedError

@CompileStatic
class PerTestRunListener {
    final String name

    private final GrailsTestEventPublisher eventPublisher
    private final JUnitReports reports
    private final SystemOutAndErrSwapper outAndErrSwapper
    private final JUnitTest testSuite

    private Long startTime
    private Integer runCount = 0
    private Integer failureCount = 0
    private Integer errorCount = 0
    private OutputStream outStream
    private OutputStream errStream

    private Map<Description,JUnit4TestCaseFacade> testsByDescription = [:]

    PerTestRunListener(String name, GrailsTestEventPublisher eventPublisher, JUnitReports reports, SystemOutAndErrSwapper outAndErrSwapper) {
        this.name = name
        this.eventPublisher = eventPublisher
        this.reports = reports
        this.outAndErrSwapper = outAndErrSwapper
        this.testSuite = new JUnitTest(name)
    }

    void start() {
        eventPublisher.testCaseStart(name)
        final streams = outAndErrSwapper.swapIn()
        outStream = streams[0]
        errStream = streams[1]
        reports.startTestSuite(testSuite)
        startTime = System.currentTimeMillis()
    }

    void finish() {
        testSuite.runTime = System.currentTimeMillis() - startTime
        testSuite.setCounts(runCount, failureCount, errorCount)
        final outAndErr = outAndErrSwapper.swapOut().collect { OutputStream out -> out.toString() }
        def out = outAndErr[0]
        def err = outAndErr[1]
        reports.systemOutput = out
        reports.systemError = err
        reports.endTestSuite(testSuite)
        eventPublisher.testCaseEnd(name)
    }

    void testStarted(Description description) {
        def testName = description.methodName
        eventPublisher.testStart(testName)
        runCount++
        for(OutputStream os in [outStream, errStream]) {
            new PrintStream(os).println("--Output from ${testName}--")
        }
        reports.startTest(getTest(description))
    }

    void testFailure(Failure failure) {
        def testName = failure.description.methodName
        def testCase = getTest(failure.description)
        def exception = failure.exception

        if (exception instanceof AssertionError) {
            eventPublisher.testFailure(testName, exception)
            failureCount++
            reports.addFailure(testCase, toAssertionFailedError(exception))
        } else {
            eventPublisher.testFailure(testName, exception, true)
            errorCount++
            reports.addError(testCase, exception)
        }
    }

    void testFinished(Description description) {
        reports.endTest(getTest(description))
        eventPublisher.testEnd(description.methodName)
    }

    // JUnitReports requires us to always pass the same Test instance
    // for a test, so we cache it; this scheme also works for the case
    // where testFailure() is invoked without a prior call to testStarted()
    private JUnit4TestCaseFacade getTest(Description description) {
        JUnit4TestCaseFacade test = testsByDescription.get(description)
        if (test == null) {
            test = createJUnit4TestCaseFacade(description)
            testsByDescription.put(description, test)
        }
        return test
    }

    private AssertionFailedError toAssertionFailedError(AssertionError assertionError) {
        def result = new AssertionFailedError(assertionError.toString())
        result.stackTrace = assertionError.getStackTrace()
        result
    }

    static JUnit4TestCaseFacade createJUnit4TestCaseFacade(Description description) {
        def ctor = JUnit4TestCaseFacade.getDeclaredConstructor(Description)
        ctor.accessible = true
        ctor.newInstance(description)
    }
}