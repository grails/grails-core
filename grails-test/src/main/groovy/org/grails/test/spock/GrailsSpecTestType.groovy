package org.grails.test.spock

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.grails.test.GrailsTestTargetPattern
import org.grails.test.event.GrailsTestRunNotifier
import org.grails.test.junit4.runner.GrailsTestCaseRunnerBuilder
import org.grails.test.spock.listener.OverallRunListener
import org.junit.runner.JUnitCore
import org.grails.test.GrailsTestTypeResult
import org.grails.test.event.GrailsTestEventPublisher
import org.grails.test.support.GrailsTestTypeSupport
import org.grails.test.report.junit.JUnitReportsFactory
import org.junit.runners.Suite
import org.spockframework.runtime.SpecUtil

import spock.config.RunnerConfiguration

import java.lang.reflect.Modifier
import org.grails.test.support.GrailsTestMode
import org.junit.runner.Request
import org.junit.runner.manipulation.Filter
import org.junit.runner.Description
import org.objenesis.ObjenesisHelper
import org.objenesis.ObjenesisBase

/**
 * @author Graeme Rocher
 */

@CompileStatic
class GrailsSpecTestType extends GrailsTestTypeSupport {
    public static final List<String> TEST_SUFFIXES = ["Test", "Tests", "Spec", "Specification"].asImmutable()

    private final List<Class> specClasses = []
    private int featureCount = 0
    protected GrailsTestMode mode

    static {
        try {
            final objStdField = ObjenesisHelper.getDeclaredField("OBJENESIS_STD")
            objStdField.accessible = true
            final objStd = objStdField.get(ObjenesisHelper)
            final cacheField = ObjenesisBase.getDeclaredField("cache")
            cacheField.accessible = true
            cacheField.set(objStd, null)
        } catch (Throwable e) {
            // ignore, failed to patch
        }

    }

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

        final reportsFactory = createJUnitReportsFactory()
        final outAndErrSwapper = createSystemOutAndErrSwapper()
        final terminalListener = createGrails2TerminalListenerIfCan()

        junit.addListener(new OverallRunListener(   eventPublisher,
                                                    reportsFactory,
                                                    outAndErrSwapper,
                                                    result,
                                                    terminalListener)   )


        optimizeSpecRunOrderIfEnabled()
        def runRequest = Request.classes(specClasses as Class[])
        GrailsTestTargetPattern[] testTargetPatterns = this.testTargetPatterns
        if(testTargetPatterns) {
            runRequest = runRequest.filterWith(new Filter() {

                @Override
                @CompileStatic
                boolean shouldRun(Description description) {
                    testTargetPatterns.any { GrailsTestTargetPattern pattern ->
                        final clsName = description.className
                        final mName = description.methodName
                        if(clsName && mName && pattern.isMethodTargeting()) {
                            pattern.matches(clsName, mName, TEST_SUFFIXES as String[])
                        }
                        else {
                            pattern.matchesClass(clsName, TEST_SUFFIXES as String[])
                        }
                    }
                }

                @Override
                @CompileStatic
                String describe() {
                    return "grails test target pattern filter"
                }
            })
        }
        junit.run(runRequest)
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