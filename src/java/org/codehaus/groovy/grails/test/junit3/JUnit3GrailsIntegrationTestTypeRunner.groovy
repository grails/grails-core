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

import org.codehaus.groovy.grails.test.GrailsTestTypeRunner
import org.codehaus.groovy.grails.test.GrailsTestTypeResult

import org.codehaus.groovy.grails.test.GrailsIntegrationTestHelper
import org.codehaus.groovy.grails.test.DefaultGrailsTestRunner

import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext

import grails.util.BuildSettings

import org.codehaus.groovy.grails.test.report.junit.JUnitReportsFactory

/**
 * Adapts the new GrailsTestTypeRunner to the pre Grails 1.2 test running API.
 */
class JUnit3GrailsIntegrationTestTypeRunner extends JUnit3GrailsTestTypeRunner {
    
    JUnit3GrailsIntegrationTestTypeRunner(String type, List<String> testNames, BuildSettings settings, ClassLoader classLoader, Closure resourceResolver, JUnitReportsFactory reportsFactory, GrailsWebApplicationContext appContext) {
        super(
            type,
            testNames,
            new GrailsIntegrationTestHelper(settings, classLoader, resourceResolver, appContext),
            new DefaultGrailsTestRunner(reportsFactory)
        )
    }

}