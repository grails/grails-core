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

package org.codehaus.groovy.grails.test.junit4.listener

import org.codehaus.groovy.grails.test.io.SystemOutAndErrSwapper
import org.codehaus.groovy.grails.test.event.GrailsTestEventPublisher
import org.codehaus.groovy.grails.test.report.junit.JUnitReportsFactory

import org.junit.runner.Description
import org.junit.runner.Result
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener

/**
 * Listens in on the execution of an entire test suite (or test "type" in Grails) and creates
 * new TestRunListeners for each actual test class.
 */
class SuiteRunListener extends RunListener {

    private final GrailsTestEventPublisher eventPublisher
    private final JUnitReportsFactory reportsFactory
    private final SystemOutAndErrSwapper outAndErrSwapper

    private PerTestRunListener perTestListener

    SuiteRunListener(GrailsTestEventPublisher eventPublisher, JUnitReportsFactory reportsFactory, SystemOutAndErrSwapper outAndErrSwapper) {
        this.eventPublisher = eventPublisher
        this.reportsFactory = reportsFactory
        this.outAndErrSwapper = outAndErrSwapper
    }

    void testRunStarted(Description description) {
        // nothing to do
    }

    void testStarted(Description description) {
        getPerTestRunListener(description).testStarted(description)
    }

    void testFailure(Failure failure) {
        getPerTestRunListener(failure.description).testFailure(failure)
    }

    void testAssumptionFailure(Failure failure) {
        // assumptions (and AssumptionViolatedException) are specific to JUnit,
        // and are treated as ordinary failures
        getPerTestRunListener(description).testFailure(failure)
    }

    void testFinished(Description description) {
        getPerTestRunListener(description).testFinished(description)
    }

    void testRunFinished(Result result) {
        getPerTestRunListener().finish()
    }

    void testIgnored(Description description) {
        // nothing to do
    }

    private getPerTestRunListener(description = null) {
        if (description && perTestListener?.name != description.className) {
            perTestListener?.finish()

            def testName = description.className
            perTestListener = new PerTestRunListener(testName, eventPublisher, reportsFactory.createReports(testName), outAndErrSwapper)
            perTestListener.start()
        }
        perTestListener
    }
}