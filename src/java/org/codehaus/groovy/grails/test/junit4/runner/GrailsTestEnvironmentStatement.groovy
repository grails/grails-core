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

class GrailsTestEnvironmentStatement extends Statement {

	private testStatement
	private target
	private mode
	private autowirer
	private requestEnvironmentInterceptor
	private transactionInterceptor
	
	GrailsTestEnvironmentStatement(Statement testStatement, Object target, GrailsTestMode mode, autowirer, requestEnvironmentInterceptor, transactionInterceptor) {
		this.testStatement = testStatement
		this.target = target
		this.mode = mode
		this.autowirer = autowirer
		this.requestEnvironmentInterceptor = requestEnvironmentInterceptor
		this.transactionInterceptor = transactionInterceptor
	}
	
	void evaluate() throws Throwable {
		autowireIfNecessary(target)
		def runner = { -> testStatement.evaluate() }
		runner = wrapInTransactionIfNecessary(runner)
		runner = wrapInRequestEnvironmentIfNecessary(runner)
		
		runner()
	}
	
	protected wrapInTransactionIfNecessary(Closure body) {
		if (mode?.wrapInTransaction && transactionInterceptor.isTransactional(target)) {
			{ -> transactionInterceptor.doInTransaction(body) }
		} else {
			body
		}
	}
	
	protected wrapInRequestEnvironmentIfNecessary(Closure body) {
		if (mode?.wrapInRequestEnvironment) {
			def controllerName = ControllerNameExtractor.extractControllerNameFromTestClassName(target.class.name, JUnit4GrailsTestType.SUFFIXES as String[])
			if (controllerName) {
				{ -> requestEnvironmentInterceptor.doInRequestEnvironment(controllerName, body) }
			} else {
				{ -> requestEnvironmentInterceptor.doInRequestEnvironment(body) }
			}
		} else {
			body
		}
	}
	
	protected autowireIfNecessary(test) {
		if (mode?.autowire) {
			autowirer.autowire(test)
		}
		test
	}
}