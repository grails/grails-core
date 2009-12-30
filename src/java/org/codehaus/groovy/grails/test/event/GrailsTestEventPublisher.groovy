/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.groovy.grails.test.event;

/**
 * Publishes test related events to the Grails build system.
 */
class GrailsTestEventPublisher {

    protected Closure event
    
    /**
     * @param event the "event" closure from the Grails build system
     */
    GrailsTestEventPublisher(Closure event) {
        this.event = event
    }
    
    /**
     * Signifies the start of a "unit" of tests. (e.g. A JUnit TestCase).
     * 
     * Must be called first, or after {@link #testCaseEnd(String,String,String)}.
     * 
     * Publishes the event {@code TestCaseStart} with {@code name} as the parameter.
     * 
     * @param name a logical name for the test "unit"
     */
    void testCaseStart(String name) {
        event("TestCaseStart", [name])
    }

    /**
     * Signifies the start of an individual test, inside a parent "unit".
     * 
     * Must be called after {@link testCaseStart(String)} or after {@link testEnd(String)}.
     * 
     * Publishes the event {@code TestStart} with {@code name} as the parameter.
     * 
     * @param name a logical name for the test
     */
    void testStart(String name) {
        event("TestStart", [name])
    }

    /**
     * Signifies that a test did not complete successfully.
     * 
     * Must be called after {@link testStart(String)} with the <em>same</em> {@code name}.
     * 
     * Publishes the event {@code TestFailure} with {@code name}, {@code failure} and {@code isError} as the parameters.
     * 
     * @param name a logical name for the test
     * @param failure the throwable raised from the failure
     * @param isError true if this failure was due to an indirect error,
     *                false if this failure was a direct assertion failure or incorrect assumption
     */
    void testFailure(String name, Throwable failure, boolean isError = false) {
        event("TestFailure", [name, failure, isError])
    }

    /**
     * Signifies that a test did not complete successfully.
     * 
     * Must be called after {@link testStart(String)} with the <em>same</em> name.
     * 
     * Publishes the event {@code TestFailure} with {@code name}, {@code failure} and {@code isError} as the parameters.
     * 
     * @param name a logical name for the test
     * @param failure a description of the failure
     * @param isError true if this failure was due to an indirect error,
     *                false if this failure was a direct assertion failure or incorrect assumption
     */
    void testFailure(String name, String failure = null, boolean isError = false) {
        event("TestFailure", [name, failure, isError])
    }

    /**
     * Signifies that a test has ended.
     * 
     * Must be called after {@link testStart(String)} with the <em>same</em> name,
     * or {@link testStart(String,String,boolean)} or {@link testStart(String,Throwable,boolean)}
     * with the same name.
     * 
     * Publishes the event {@code TestEnd} with {@code name} as the parameter.
     * 
     * @param name a logical name for the test
     * @param failure a description of the failure
     * @param isError true if this failure was due to an indirect error,
     *                false if this failure was a direct assertion failure or incorrect assumption
     */
    void testEnd(String name) {
        event("TestEnd", [name])
    }

    /**
     * Signifies the end of a "unit" of tests. (e.g. A JUnit TestCase).
     * 
     * Must be called after {@link testEnd(String)} with the <em>same</em> name as
     * the most recent call to {@link testCaseStart(String)}.
     * 
     * Publishes the event {@code TestCaseEnd} with {@code name} as the parameter.
     * 
     * @param name a logical name for the test "unit"
     */
    void testCaseEnd(String name, String out = null, String err = null) {
        event("TestCaseEnd", [name, out, err])
    }
    
}