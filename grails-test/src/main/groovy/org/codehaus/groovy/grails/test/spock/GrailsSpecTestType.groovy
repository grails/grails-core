package org.codehaus.groovy.grails.test.spock

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.grails.test.GrailsTestTargetPattern
import org.codehaus.groovy.grails.test.event.GrailsTestRunNotifier
import org.codehaus.groovy.grails.test.junit4.runner.GrailsTestCaseRunnerBuilder
import org.codehaus.groovy.grails.test.spock.listener.OverallRunListener

/**
 * @author Graeme Rocher
 */

import org.junit.runner.JUnitCore
import org.codehaus.groovy.grails.test.GrailsTestTypeResult
import org.codehaus.groovy.grails.test.event.GrailsTestEventPublisher
import org.codehaus.groovy.grails.test.support.GrailsTestTypeSupport
import org.codehaus.groovy.grails.test.report.junit.JUnitReportsFactory
import org.junit.runners.Suite
import org.spockframework.runtime.SpecUtil

import spock.config.RunnerConfiguration

import java.lang.reflect.Modifier
import org.codehaus.groovy.grails.test.support.GrailsTestMode

@CompileStatic
class GrailsSpecTestType extends GrailsTestTypeSupport {
    public static final List<String> TEST_SUFFIXES = ["Test", "Tests", "Spec", "Specification"].asImmutable()

    private final List<Class> specClasses = []
    private int featureCount = 0
    protected GrailsTestMode mode

    GrailsSpecTestType(String name, String relativeSourcePath) {
        super(name, relativeSourcePath)
    }

    GrailsSpecTestType(String name, String relativeSourcePath, GrailsTestMode mode) {
        super(name, relativeSourcePath)
        this.mode = mode
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
            if(!Modifier.isAbstract(specClass.getModifiers()))
                specClasses << specClass
        }
        def testClasses = specClasses
        if (testClasses) {
            Suite suite = createSuite(specClasses)
            featureCount = suite.testCount()
        }
        else {
            0
        }

        return featureCount
    }

    protected Suite createSuite(classes) {
        if(mode) {
            new Suite(new GrailsTestCaseRunnerBuilder(mode, getApplicationContext(), testTargetPatterns), classes as Class[])
        }
        else {
            new Suite(new GrailsTestCaseRunnerBuilder(testTargetPatterns), classes as Class[])
        }
    }


    @CompileStatic(TypeCheckingMode.SKIP)
    protected GrailsTestTypeResult doRun(GrailsTestEventPublisher eventPublisher) {
        def junit = new JUnitCore()
        def result = new GrailsSpecTestTypeResult()

        junit.addListener(new OverallRunListener(eventPublisher,
            createJUnitReportsFactory(), createSystemOutAndErrSwapper(), result,
            createGrails2TerminalListenerIfCan()))


        optimizeSpecRunOrderIfEnabled()
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