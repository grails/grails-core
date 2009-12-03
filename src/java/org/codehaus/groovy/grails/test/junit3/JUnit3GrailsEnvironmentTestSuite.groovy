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

import junit.framework.Test
import junit.framework.TestResult
import junit.framework.TestSuite

import org.springframework.context.ApplicationContext

import java.util.regex.Matcher
import java.util.regex.Pattern

import org.codehaus.groovy.grails.test.support.GrailsTestAutowirer
import org.codehaus.groovy.grails.test.support.GrailsTestRequestEnvironmentInterceptor
import org.codehaus.groovy.grails.test.support.GrailsTestTransactionInterceptor

/**
 * A Grails specific test suite that runs tests in a “Grails” environment. That is,
 * subjects the tests to autowiring of dependencies (by name), establishes a request like environment,
 * and inside a rollback only transaction (unless the test is not transactional).
 * 
 * @see GrailsTestAutowirer
 * @see GrailsTestRequestEnvironmentInterceptor
 * @see GrailsTestTransactionInterceptor
 */
class JUnit3GrailsEnvironmentTestSuite extends TestSuite {
    
    final GrailsTestAutowirer autowirer
    final GrailsTestRequestEnvironmentInterceptor requestEnvironmentInterceptor
    final GrailsTestTransactionInterceptor transactionInterceptor
    
	final JUnit3GrailsTestTypeMode mode
	
    /**
     * @see TestSuite(Class)
     */
    JUnit3GrailsEnvironmentTestSuite(Class test, ApplicationContext applicationContext, JUnit3GrailsTestTypeMode mode) {
        super(test)
        init(applicationContext, mode)
    }

    JUnit3GrailsEnvironmentTestSuite(ApplicationContext applicationContext, JUnit3GrailsTestTypeMode mode) {
        init(applicationContext, mode)
    }

    protected init(ApplicationContext applicationContext, JUnit3GrailsTestTypeMode mode) {
        this.mode = mode
        autowirer = new GrailsTestAutowirer(applicationContext)
        requestEnvironmentInterceptor = new GrailsTestRequestEnvironmentInterceptor(applicationContext)
        transactionInterceptor = new GrailsTestTransactionInterceptor(applicationContext)
    }
    
    void runTest(Test test, TestResult result) {
        if (mode.autowire) {
            autowirer.autowire(test)
        }

        def rawRunner = { test.run(result) }
        def inTransactionRunner = mode.wrapInTransaction && transactionInterceptor.isTransactional(test) ? { transactionInterceptor.doInTransaction(rawRunner) } : rawRunner 
        def inRequestRunner = mode.wrapInRequestEnvironment ? { requestEnvironmentInterceptor.doInRequestEnvironment(transactionRunner) } : rawRunner
        
        inRequestRunner()
    }
}
