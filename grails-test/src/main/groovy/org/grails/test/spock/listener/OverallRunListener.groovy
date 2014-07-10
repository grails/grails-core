package org.grails.test.spock.listener

import groovy.transform.CompileStatic
import org.grails.test.event.GrailsTestRunNotifier

/**
 * @author Luke Daley
 * @author Graeme Rocher
 * @since 2.3
 */
import org.grails.test.report.junit.JUnitReportsFactory
import org.grails.test.spock.GrailsSpecTestTypeResult
import org.grails.test.event.GrailsTestEventPublisher
import org.grails.test.io.SystemOutAndErrSwapper
import org.junit.runner.Description
import org.junit.runner.Result
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener

@CompileStatic
class OverallRunListener extends RunListener {
    private final GrailsTestEventPublisher eventPublisher
    private final JUnitReportsFactory reportsFactory
    private final SystemOutAndErrSwapper outAndErrSwapper
    private final GrailsSpecTestTypeResult result
    private final GrailsTestRunNotifier grailsNotifier
    private PerSpecRunListener perSpecListener

    OverallRunListener(GrailsTestEventPublisher eventPublisher, JUnitReportsFactory reportsFactory, SystemOutAndErrSwapper outAndErrSwapper, GrailsSpecTestTypeResult result, GrailsTestRunNotifier grailsNotifier) {
        this.eventPublisher = eventPublisher
        this.reportsFactory = reportsFactory
        this.outAndErrSwapper = outAndErrSwapper
        this.result = result
        this.grailsNotifier = grailsNotifier
    }

    void testRunStarted(Description description) {
        // nothing to do
    }

    void testStarted(Description description) {
        getPerSpecRunListener(description).testStarted(description)
    }

    void testFailure(Failure failure) {
        getPerSpecRunListener(failure.description).testFailure(failure)
    }

    void testAssumptionFailure(Failure failure) {
        // assumptions (and AssumptionViolatedException) are specific to JUnit,
        // and are treated as ordinary failures
        getPerSpecRunListener(failure.description).testFailure(failure)
    }

    void testFinished(Description description) {
        getPerSpecRunListener(description).testFinished(description)
    }

    void testRunFinished(Result result) {
        // I can't think of a situation where this would be called without
        // a perSpecListener being available, even if it does happen
        // our handling options are very limited at this point in execution.
        getPerSpecRunListener()?.finish()
    }

    void testIgnored(Description description) {
        // nothing to do
    }

    private PerSpecRunListener getPerSpecRunListener(Description description = null) {
        if (description && perSpecListener?.name != description.className) {
            perSpecListener?.finish()

            def specName = description.className
            perSpecListener = new PerSpecRunListener(specName, eventPublisher,
                reportsFactory.createReports(specName), outAndErrSwapper, result,
                grailsNotifier)

            perSpecListener.start()
        }

        perSpecListener
    }
}
