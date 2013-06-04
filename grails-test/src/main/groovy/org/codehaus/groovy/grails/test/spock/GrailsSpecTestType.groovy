package org.codehaus.groovy.grails.test.spock

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.grails.test.GrailsTestTargetPattern
import org.codehaus.groovy.grails.test.event.GrailsTestRunNotifier
import org.codehaus.groovy.grails.test.spock.listener.OverallRunListener

/**
 * @author Graeme Rocher
 */

import org.junit.runner.JUnitCore
import org.codehaus.groovy.grails.test.GrailsTestTypeResult
import org.codehaus.groovy.grails.test.event.GrailsTestEventPublisher
import org.codehaus.groovy.grails.test.support.GrailsTestTypeSupport
import org.codehaus.groovy.grails.test.report.junit.JUnitReportsFactory

import org.spockframework.runtime.SpecUtil

import spock.config.RunnerConfiguration

@CompileStatic
class GrailsSpecTestType extends GrailsTestTypeSupport {
    public static final List<String> TEST_SUFFIXES = ["Test", "Tests", "Spec", "Specification"].asImmutable()

    private final List<Class> specClasses = []
    private int featureCount = 0

    GrailsSpecTestType(String name, String relativeSourcePath) {
        super(name, relativeSourcePath)
    }

    protected List<String> getTestExtensions() {
        ["groovy"]
    }

    protected List<String> getTestSuffixes() {
        TEST_SUFFIXES
    }

    JUnitReportsFactory createJUnitReportsFactory() {
        JUnitReportsFactory.createFromBuildBinding(buildBinding)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected int doPrepare() {
        eachSourceFile { GrailsTestTargetPattern testTargetPattern, File specSourceFile ->
            def specClass = sourceFileToClass(specSourceFile)
            if (SpecUtil.isRunnableSpec(specClass)) specClasses << specClass
        }

        optimizeSpecRunOrderIfEnabled()

        featureCount = (Integer)specClasses.sum(0) { Class spec -> SpecUtil.getFeatureCount(spec) }
        featureCount
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected GrailsTestTypeResult doRun(GrailsTestEventPublisher eventPublisher) {
        def junit = new JUnitCore()
        def result = new GrailsSpecTestTypeResult()

        try {
            junit.addListener(new OverallRunListener(eventPublisher,
                createJUnitReportsFactory(), createSystemOutAndErrSwapper(), result,
                createGrails2TerminalListenerIfCan()))
        } catch (Throwable e) {

            println e.message
        }

        junit.run(specClasses as Class[])
        result
    }

    private void optimizeSpecRunOrderIfEnabled() {
        if (!SpecUtil.getConfiguration(RunnerConfiguration).optimizeRunOrder) return

        def specNames = specClasses.collect { Class it -> it.name }
        def reordered = SpecUtil.optimizeRunOrder(specNames)
        def reordedPositions = [:]
        reordered.eachWithIndex { String name, int idx -> reordedPositions[name] = idx }
        specClasses.sort { Class it -> reordedPositions[it.name] }
    }

    // Override to workaround GRAILS-7296
    @CompileStatic(TypeCheckingMode.SKIP)
    protected File getSourceDir() {
        new File(buildBinding.grailsSettings.testSourceDir, relativeSourcePath)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected createGrails2TerminalListenerIfCan() {
        return new GrailsTestRunNotifier(featureCount)
    }
}