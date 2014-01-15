/*
 * Copyright 2012 the original author or authors.
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

package grails.test.mixin.integration

import groovy.transform.CompileStatic
import junit.framework.AssertionFailedError
import org.codehaus.groovy.runtime.ScriptBytecodeAdapter
import org.junit.Before
import org.codehaus.groovy.grails.test.support.GrailsTestInterceptor
import grails.test.mixin.TestMixinTargetAware
import org.codehaus.groovy.grails.test.support.GrailsTestMode
import org.junit.After
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.grails.test.runner.phase.IntegrationTestPhaseConfigurer

/**
 * A mixin for enhancing integration tests with autowiring and transactional capabitities
 */
@CompileStatic
class IntegrationTestMixin implements TestMixinTargetAware {

    Object target
    GrailsTestInterceptor interceptor

    @CompileStatic(TypeCheckingMode.SKIP)
    void setTarget(Object target) {
        this.target = target
        try {
            final applicationContext = IntegrationTestPhaseConfigurer.currentApplicationContext
            if(applicationContext && target) {
                interceptor = new GrailsTestInterceptor(target, new GrailsTestMode( autowire: true,
                                                                                    wrapInRequestEnvironment: true,
                                                                                    wrapInTransaction: target.hasProperty('transactional') ? target['transactional'] : true),
                                                                                    applicationContext,
                                                                                    ['Spec', 'Specification','Test', 'Tests'] as String[] )
            }
        } catch (IllegalStateException ise) {
            // ignore, thrown when application context hasn't been bootstrapped
        }
    }

    @Before
    void initIntegrationTest() {
        interceptor?.init()
    }

    @After
    void destoryIntegrationTest() {
        interceptor?.destroy()
    }

    /**
     * Asserts that the given code closure fails when it is evaluated
     *
     * @param code
     * @return the message of the thrown Throwable
     */
    String shouldFail(Closure code) {
        boolean failed = false
        String result = null
        try {
            code.call()
        }
        catch (GroovyRuntimeException gre) {
            failed = true
            result = ScriptBytecodeAdapter.unwrap(gre).getMessage()
        }
        catch (Throwable e) {
            failed = true
            result = e.getMessage()
        }
        if (!failed) {
            throw new AssertionFailedError("Closure " + code + " should have failed")
        }

        return result
    }

    /**
     * Asserts that the given code closure fails when it is evaluated
     * and that a particular exception is thrown.
     *
     * @param clazz the class of the expected exception
     * @param code the closure that should fail
     * @return the message of the expected Throwable
     */
    String shouldFail(Class clazz, Closure code) {
        Throwable th = null
        try {
            code.call()
        } catch (GroovyRuntimeException gre) {
            th = ScriptBytecodeAdapter.unwrap(gre)
        } catch (Throwable e) {
            th = e
        }

        if (th == null) {
            throw new AssertionFailedError("Closure $code should have failed with an exception of type $clazz.name")
        }

        if (!clazz.isInstance(th)) {
            throw new AssertionFailedError("Closure $code should have failed with an exception of type $clazz.name, instead got Exception $th")
        }

        return th.message
    }
}
