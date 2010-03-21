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

import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement
import org.junit.runner.notification.RunNotifier
import org.junit.internal.runners.statements.InvokeMethod

import org.springframework.context.ApplicationContext

import org.codehaus.groovy.grails.test.support.GrailsTestMode
import org.codehaus.groovy.grails.test.support.GrailsTestAutowirer
import org.codehaus.groovy.grails.test.support.GrailsTestRequestEnvironmentInterceptor
import org.codehaus.groovy.grails.test.support.GrailsTestTransactionInterceptor

import org.junit.internal.runners.statements.RunAfters
import org.junit.internal.runners.statements.RunBefores
import org.springframework.util.ReflectionUtils

class GrailsTestCaseRunner extends BlockJUnit4ClassRunner {

	final mode
	final appCtx

	private autowirer
	private requestEnvironmentInterceptor
	private transactionInterceptor
	
	private transactional = false
	private controllerName = null

	GrailsTestCaseRunner(Class testClass) {
		this(testClass, null, null)
	}
	
	GrailsTestCaseRunner(Class testClass, GrailsTestMode mode, ApplicationContext appCtx) {
		super(testClass)
		this.mode = mode
		this.appCtx = appCtx
		validateMode()
	}
	
	protected validateMode() {
		if (mode && appCtx == null) {
			throw new IllegalStateException("mode $mode requires an application context")
		}
	}
	
	protected Statement methodInvoker(FrameworkMethod method, Object test) {
		if (mode) {
			new GrailsTestInvokeMethod(method, test, mode, getRequestEnvironmentInterceptor(), getTransactionInterceptor())
		} else {
			new InvokeMethod(method, test)
		}
	}
	
	protected createTest() {
		autowireIfNecessary(super.createTest())
	}
	
	protected autowireIfNecessary(test) {
		if (mode?.autowire) {
			getAutowirer().autowire(test)
		}
		test
	}
	
	protected getAutowirer() {
		ifHasApplicationContext {
			autowirer = this.@autowirer ?: new GrailsTestAutowirer(appCtx)
		}
	}

	protected getTransactionInterceptor() {
		ifHasApplicationContext {
			transactionInterceptor = this.@transactionInterceptor ?: new GrailsTestTransactionInterceptor(appCtx)
		}
	}

	protected getRequestEnvironmentInterceptor() {
		ifHasApplicationContext {
			requestEnvironmentInterceptor = this.@requestEnvironmentInterceptor ?: new GrailsTestRequestEnvironmentInterceptor(appCtx)
		}
	}

	protected ifHasApplicationContext(body) {
		appCtx ? body() : null
	}

	protected List<FrameworkMethod> computeTestMethods() {
		def annotated = super.computeTestMethods()
		testClass.javaClass.methods.each { method ->
			if (method.name.size() > 4 && method.name[0..3] == "test" && method.parameterTypes.size() == 0) {
				def existing = annotated.find { it.method == method }
				if (!existing) {
					annotated << new FrameworkMethod(method)
				}
			}
		}
		annotated
	}

	protected Statement withBefores(FrameworkMethod method, Object target, Statement statement) {
		def superResult = super.withBefores(method, target, statement)
		if (superResult.is(statement)) {
            def setupMethod = ReflectionUtils.findMethod(testClass.javaClass, 'setUp')
            if(setupMethod) {
                setupMethod.accessible = true
                def setUp = new FrameworkMethod(setupMethod)
                new RunBefores(statement, [setUp], target)
            }
            else {
                superResult
            }
		} else {
			superResult
		}
	}
	
	protected Statement withAfters(FrameworkMethod method, Object target, Statement statement) {
		def superResult = super.withAfters(method, target, statement)
		if (superResult.is(statement)) {
            def tearDownMethod = ReflectionUtils.findMethod(testClass.javaClass, 'tearDown')
			if(tearDownMethod) {
                tearDownMethod.accessible = true
				def tearDown = new FrameworkMethod(tearDownMethod)
				new RunAfters(statement, [tearDown], target)
			} else {
				superResult
			}
		} else {
			superResult
		}
	}
}