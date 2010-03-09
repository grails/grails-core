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

import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement

import org.codehaus.groovy.grails.test.support.GrailsTestMode
import org.codehaus.groovy.grails.test.support.ControllerNameExtractor
import org.codehaus.groovy.grails.test.junit4.JUnit4GrailsTestType
import org.springframework.context.ApplicationContext

class GrailsTestInvokeMethod extends Statement {

	private testMethod
	private target
	private mode
	private autowirer
	private requestEnvironmentInterceptor
	private transactionInterceptor
	
	GrailsTestInvokeMethod(FrameworkMethod testMethod, Object target, GrailsTestMode mode, autowirer, requestEnvironmentInterceptor, transactionInterceptor) {
		this.testMethod = testMethod
		this.target = target
		this.mode = mode
		this.autowirer = autowirer
		this.requestEnvironmentInterceptor = requestEnvironmentInterceptor
		this.transactionInterceptor = transactionInterceptor
	}
	
	void evaluate() throws Throwable {
		if (mode.autowire) {
			autowirer.autowire(target)
		}
		
		def runner = { testMethod.invokeExplosively(target) }
		
		def inTransactionRunner
		if (mode.wrapInTransaction && transactionInterceptor.isTransactional(target)) {
			inTransactionRunner = { transactionInterceptor.doInTransaction(runner) }
		} else {
			inTransactionRunner = runner
		}

		def inRequestRunner
		if (mode.wrapInRequestEnvironment) {
			def controllerName = ControllerNameExtractor.extractControllerNameFromTestClassName(target.class.name, JUnit4GrailsTestType.SUFFIXES as String[])
			if (controllerName) {
				inRequestRunner = { requestEnvironmentInterceptor.doInRequestEnvironment(controllerName, inTransactionRunner) }
			} else {
				inRequestRunner = { requestEnvironmentInterceptor.doInRequestEnvironment(inTransactionRunner) }
			}
		} else {
			inRequestRunner = inTransactionRunner
		}

		inRequestRunner()
	}
	

}