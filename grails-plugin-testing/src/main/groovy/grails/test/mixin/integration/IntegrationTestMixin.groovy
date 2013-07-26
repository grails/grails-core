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
import org.junit.Before
import org.codehaus.groovy.grails.test.support.GrailsTestInterceptor
import grails.test.mixin.TestMixinTargetAware
import grails.util.Holders
import org.codehaus.groovy.grails.test.support.GrailsTestMode
import org.junit.After

/**
 * A mixin for enhancing integration tests with autowiring and transactional capabitities
 */
@CompileStatic
class IntegrationTestMixin implements TestMixinTargetAware {

    Object target
    GrailsTestInterceptor interceptor

    void setTarget(Object target) {
        this.target = target
        try {
            final applicationContext = Holders.getApplicationContext()
            if(applicationContext && target) {
                interceptor = new GrailsTestInterceptor(target, new GrailsTestMode(autowire: true, wrapInRequestEnvironment: true, wrapInTransaction: true), applicationContext, ['Spec', 'Specification','Test', 'Tests'] as String[])
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
