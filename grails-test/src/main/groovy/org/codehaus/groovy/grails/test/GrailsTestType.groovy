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

package org.codehaus.groovy.grails.test

import org.codehaus.groovy.grails.test.event.GrailsTestEventPublisher

/**
 * Describes the contract that a test type must support to be 
 * runnable by `grails test-app`.
 */
interface GrailsTestType {

    /**
     * A suitable display name for this test type.
     * 
     * Can be called at any time.
     */
    String getName()
    
    /**
     * The relative path from the configured test source directory to the particular directory 
     * that contains the tests for this test type.
     * 
     * The build will compile the source in directory returned by this if it is not null and exists.
     * 
     * @return the directory to compile relative to the build test directory, or {@code null} if there is nothing to compile.
     */
    String getRelativeSourcePath()

    /**
     * Perform any kind of initialisation, and return how many tests will be run.
     * 
     * If the value returned is less than 1, {@link #run(GrailsTestEventPublisher)} will NOT be called.
     * 
     * @param compiledClassesDir where the source was compiled to, or {@code null} if 
     *        {@link getRelativeSourcePath()} returned {@code null}.
     * @param buildBinding the binding from the build environment
     * @return the number of tests of this type.
     */
    int prepare(GrailsTestTargetPattern[] testTargetPatterns, File compiledClassesDir, Binding buildBinding)
        
    /**
     * Runs the tests, appropriately calls {@link GrailsTestEventPublisher eventPublisher} and 
     * returns the {@link GrailsTestTypeResult test result}.
     */
    GrailsTestTypeResult run(GrailsTestEventPublisher eventPublisher)

    /**
     * Do any necessary tidy up.
     */
    void cleanup() 
}