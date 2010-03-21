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

package org.codehaus.groovy.grails.test.junit4.runner

import org.codehaus.groovy.grails.test.junit4.JUnit4GrailsTestType

import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement
import org.junit.runner.notification.RunNotifier
import org.junit.internal.runners.statements.RunAfters
import org.junit.internal.runners.statements.RunBefores
import org.junit.internal.runners.statements.Fail

import org.springframework.context.ApplicationContext
import org.springframework.util.ReflectionUtils

import org.codehaus.groovy.grails.test.support.GrailsTestMode
import org.codehaus.groovy.grails.test.support.GrailsTestInterceptor

import java.lang.reflect.InvocationTargetException

class GrailsTestCaseRunner extends BlockJUnit4ClassRunner {

    final mode
    final appCtx

    GrailsTestCaseRunner(Class testClass) {
        this(testClass, null, null)
    }

    GrailsTestCaseRunner(Class testClass, GrailsTestMode mode, ApplicationContext appCtx) {
        super(testClass)
        this.mode = mode
        this.appCtx = appCtx
        validateMode()
    }

    protected validateMode() {
        if (mode && appCtx == null) {
            throw new IllegalStateException("mode $mode requires an application context")
        }
    }

    /**
     * This is the only suitable hook that allows us to wrap the before/after
     * methods in transactions etc. Unfortunately, that means we have to copy
     * most the implementation from BlockJUnit4ClassRunner.
     */
    protected Statement methodBlock(FrameworkMethod method) {
        if (mode) {
            def test = null

            // Create test instantiates the test object reflectively, so we unwrap
            // the InvocationTargetException if an exception occurs.
            try {
                test = createTest()
            } catch (InvocationTargetException e) {
                return new Fail(e.targetException)
            }

            def statement = methodInvoker(method, test)
            statement = possiblyExpectingExceptions(method, test, statement)
            statement = withPotentialTimeout(method, test, statement)
            statement = withBefores(method, test, statement)
            statement = withAfters(method, test, statement)
            statement = withRules(method, test, statement)

            withGrailsTestEnvironment(statement, test)
        } else {
            // fast lane for unit tests
            super.methodBlock(method)
        }
    }

    protected withGrailsTestEnvironment(Statement statement, Object test) {
        if (!mode) {
            throw new IllegalStateException("withGrailsTestEnvironment can not be called without a test mode set")
        }

        def interceptor = mode.createInterceptor(test, appCtx, JUnit4GrailsTestType.SUFFIXES as String[])
        new GrailsTestEnvironmentStatement(statement, test, interceptor)
    }

    protected List<FrameworkMethod> computeTestMethods() {
        def annotated = super.computeTestMethods()
        testClass.javaClass.methods.each { method ->
            if (method.name.size() > 4 && method.name[0..3] == "test" && method.parameterTypes.size() == 0) {
                def existing = annotated.find { it.method == method }
                if (!existing) {
                    annotated << new FrameworkMethod(method)
                }
            }
        }
        annotated
    }

    protected Statement withBefores(FrameworkMethod method, Object target, Statement statement) {
        def superResult = super.withBefores(method, target, statement)
        if (superResult.is(statement)) {
            def setupMethod = ReflectionUtils.findMethod(testClass.javaClass, 'setUp')
            if(setupMethod) {
                setupMethod.accessible = true
                def setUp = new FrameworkMethod(setupMethod)
                new RunBefores(statement, [setUp], target)
            }
            else {
                superResult
            }
        } else {
            superResult
        }
    }

    protected Statement withAfters(FrameworkMethod method, Object target, Statement statement) {
        def superResult = super.withAfters(method, target, statement)
        if (superResult.is(statement)) {
            def tearDownMethod = ReflectionUtils.findMethod(testClass.javaClass, 'tearDown')
            if(tearDownMethod) {
                tearDownMethod.accessible = true
                def tearDown = new FrameworkMethod(tearDownMethod)
                new RunAfters(statement, [tearDown], target)
            } else {
                superResult
            }
        } else {
            superResult
        }
    }
}