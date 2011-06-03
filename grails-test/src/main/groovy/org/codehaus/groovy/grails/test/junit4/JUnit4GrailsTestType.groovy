/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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
package org.codehaus.groovy.grails.test.junit4

import java.lang.reflect.Modifier
import org.codehaus.groovy.grails.test.GrailsTestTypeResult
import org.codehaus.groovy.grails.test.event.GrailsTestEventPublisher
import org.codehaus.groovy.grails.test.event.GrailsTestRunNotifier
import org.codehaus.groovy.grails.test.junit4.listener.SuiteRunListener
import org.codehaus.groovy.grails.test.junit4.result.JUnit4ResultGrailsTestTypeResultAdapter
import org.codehaus.groovy.grails.test.junit4.runner.GrailsTestCaseRunnerBuilder
import org.codehaus.groovy.grails.test.report.junit.JUnitReportsFactory
import org.codehaus.groovy.grails.test.support.GrailsTestMode
import org.codehaus.groovy.grails.test.support.GrailsTestTypeSupport
import org.junit.runner.Result
import org.junit.runners.Suite

/**
 * An {@code GrailsTestType} for JUnit4 tests.
 */
class JUnit4GrailsTestType extends GrailsTestTypeSupport {

    static final SUFFIXES = ["Test", "Tests"].asImmutable()

    protected suite
    protected mode

    JUnit4GrailsTestType(String name, String sourceDirectory) {
        this(name, sourceDirectory, null)
    }

    JUnit4GrailsTestType(String name, String sourceDirectory, GrailsTestMode mode) {
        super(name, sourceDirectory)
        this.mode = mode
    }

    protected List<String> getTestSuffixes() { SUFFIXES }

    protected int doPrepare() {
        def testClasses = getTestClasses()
        if (testClasses) {
            suite = createSuite(testClasses)
            suite.testCount()
        }
        else {
            0
        }
    }

    protected getTestClasses() {
        def classes = []
        eachSourceFile { testTargetPattern, sourceFile ->
            def testClass = sourceFileToClass(sourceFile)
            if (!Modifier.isAbstract(testClass.modifiers)) {
                classes << testClass
            }
        }
        classes
    }

    protected createRunnerBuilder() {
        if (mode) {
            new GrailsTestCaseRunnerBuilder(mode, getApplicationContext(), testTargetPatterns)
        }
        else {
            new GrailsTestCaseRunnerBuilder(testTargetPatterns)
        }
    }

    protected createSuite(classes) {
        new Suite(createRunnerBuilder(), classes as Class[])
    }

    protected createJUnitReportsFactory() {
        JUnitReportsFactory.createFromBuildBinding(buildBinding)
    }

    protected createListener(eventPublisher) {
        new SuiteRunListener(eventPublisher, createJUnitReportsFactory(), createSystemOutAndErrSwapper())
    }

    protected createNotifier(eventPublisher) {
        int total = suite.children.collect { it.children.size()}.sum()
        def notifier = new GrailsTestRunNotifier(total)
        notifier.addListener(createListener(eventPublisher))
        notifier
    }

    protected GrailsTestTypeResult doRun(GrailsTestEventPublisher eventPublisher) {
        def notifier = createNotifier(eventPublisher)
        def result = new Result()
        notifier.addListener(result.createListener())

        suite.run(notifier)

        notifier.fireTestRunFinished(result)
        new JUnit4ResultGrailsTestTypeResultAdapter(result)
    }
}
