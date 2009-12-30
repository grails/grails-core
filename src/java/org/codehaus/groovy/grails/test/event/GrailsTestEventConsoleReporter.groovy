/* Copyright 2009 the original author or authors.
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

package org.codehaus.groovy.grails.test.event

import grails.build.GrailsBuildListener

class GrailsTestEventConsoleReporter implements GrailsBuildListener {

    private final static EVENTS = ['TestCaseStart', 'TestFailure', 'TestCaseEnd']
        
    protected PrintStream out
    protected int failureCount
    
    GrailsTestEventConsoleReporter(PrintStream out) {
        this.out = out
    }
    
    void receiveGrailsBuildEvent(String name, Object[] args) {
        if (name in EVENTS) {
            this."do$name"(*args)
        }
    }
    
    protected doTestCaseStart(String name) {
        out.print("Running test ${name}...")
        failureCount = 0
    }

    protected doTestFailure(String name, failure, boolean isError) {
        if (++failureCount == 1) out.println()
        out.println("                    ${name}...FAILED")
    }

    protected doTestCaseEnd(String name, String out, String err) {
        if (failureCount == 0) out.println("PASSED")
    }
}