/*
 * Copyright 2009 the original author or authors.
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
 
package org.codehaus.groovy.grails.test.junit3

import junit.framework.TestResult
import org.codehaus.groovy.grails.test.GrailsTestTypeResult

/**
 * Adapts the new GrailsTestTypeResult to the pre Grails 1.2 test running API.
 */
class JUnit3GrailsTestTypeResult implements GrailsTestTypeResult {

    protected result
    
    JUnit3GrailsTestTypeResult(TestResult result) {
        this.result = result
    }
    
    int getPassCount() {
        result.runCount() - failCount
    }
    
    int getFailCount() {
        result.errorCount() + result.failureCount()
    }
    
}