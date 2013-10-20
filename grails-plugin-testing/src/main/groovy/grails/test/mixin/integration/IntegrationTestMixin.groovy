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

import grails.test.mixin.TestMixinTargetAware
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import org.codehaus.groovy.grails.test.runner.phase.IntegrationTestPhaseConfigurer
import org.codehaus.groovy.grails.test.support.GrailsTestInterceptor
import org.codehaus.groovy.grails.test.support.GrailsTestMode
import org.junit.After
import org.junit.Before

/**
 * A mixin for enhancing integration tests with autowiring and transactional capabitities
 */
@CompileStatic
class IntegrationTestMixin implements TestMixinTargetAware {

    Object target
    GrailsTestInterceptor interceptor

    @CompileStatic(TypeCheckingMode.SKIP)
    void setTarget(target) {
        this.target = target
        try {
            final applicationContext = IntegrationTestPhaseConfigurer.currentApplicationContext
            if (applicationContext && target) {
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
}
