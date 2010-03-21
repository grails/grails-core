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

package org.codehaus.groovy.grails.test.support

import org.springframework.context.ApplicationContext

class GrailsTestInterceptor {

	private test
	private mode
	private appCtx
	private testClassSuffixes

	private transactionInterceptor
	private requestEnvironmentInterceptor
	
	GrailsTestInterceptor(Object test, GrailsTestMode mode, ApplicationContext appCtx, String[] testClassSuffixes) {
		this.test = test
		this.mode = mode
		this.appCtx = appCtx
		this.testClassSuffixes = testClassSuffixes
	}

	void init() {
		autowireIfNecessary()
		initTransactionIfNecessary()
		initRequestEnvironmentIfNecessary()
	}
	
	void destroy() {
		transactionInterceptor?.destroy()
		requestEnvironmentInterceptor?.destroy()
	}
	
	void wrap(Closure body) {
		init()
		try {
			body()
		} finally {
			destroy()
		}
	}

	protected autowireIfNecessary() {
		if (mode.autowire) createAutowirer().autowire(test)
	}
	
	protected initTransactionIfNecessary() {
		def localTransactionInterceptor = createTransactionInterceptor()
		if (mode.wrapInTransaction && localTransactionInterceptor.isTransactional(test)) {
			transactionInterceptor = localTransactionInterceptor
			transactionInterceptor.init()
		}
	}
	
	protected destroyTransactionIfNecessary() {
		if (transactionInterceptor) {
			transactionInterceptor.destroy()
			transactionInterceptor = null
		}
	}
	
	protected getControllerName() {
		ControllerNameExtractor.extractControllerNameFromTestClassName(test.class.name, testClassSuffixes)
	}

	protected initRequestEnvironmentIfNecessary(Closure body) {
		if (mode.wrapInRequestEnvironment) {
			requestEnvironmentInterceptor = createRequestEnvironmentInterceptor()
			def controllerName = getControllerName()
			controllerName ? requestEnvironmentInterceptor.init(controllerName) : requestEnvironmentInterceptor.init()
		}
	}
	
	protected destroyRequestEnvironmentIfNecessary() {
		if (requestEnvironmentInterceptor) {
			requestEnvironmentInterceptor.destroy()
			requestEnvironmentInterceptor = null
		}
	}

	protected createAutowirer() {
		new GrailsTestAutowirer(appCtx)
	}

	protected createTransactionInterceptor() {
		new GrailsTestTransactionInterceptor(appCtx)
	}

	protected createRequestEnvironmentInterceptor() {
		new GrailsTestRequestEnvironmentInterceptor(appCtx)
	}

}