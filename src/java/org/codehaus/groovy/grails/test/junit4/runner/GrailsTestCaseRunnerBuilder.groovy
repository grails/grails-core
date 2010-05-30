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

import org.junit.runner.Runner
import org.junit.runners.model.RunnerBuilder

import org.codehaus.groovy.grails.test.GrailsTestTargetPattern
import org.codehaus.groovy.grails.test.support.GrailsTestMode
import org.springframework.context.ApplicationContext

class GrailsTestCaseRunnerBuilder extends RunnerBuilder {

    final mode
    final appCtx
    final testTargetPatterns

    GrailsTestCaseRunnerBuilder(GrailsTestTargetPattern[] testTargetPatterns) {
        this(null, null, testTargetPatterns)
    }

    GrailsTestCaseRunnerBuilder(GrailsTestMode mode, ApplicationContext appCtx, GrailsTestTargetPattern[] testTargetPatterns) {
        this.mode = mode
        this.appCtx = appCtx
        this.testTargetPatterns = testTargetPatterns
        validateMode()
    }

    protected validateMode() {
        if (mode && appCtx == null) {
            throw new IllegalStateException("mode $mode requires an application context")
        }
    }

    Runner runnerForClass(Class testClass) {
        if (mode) {
            new GrailsTestCaseRunner(testClass, mode, appCtx, *testTargetPatterns)
        } else {
            new GrailsTestCaseRunner(testClass, *testTargetPatterns)
        }
    }

}