/*
 * Copyright 2012 the original author or authors.
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

package grails.test.spock

import org.grails.test.support.GrailsTestAutowirer
import org.grails.test.support.GrailsTestTransactionInterceptor
import org.grails.test.support.GrailsTestRequestEnvironmentInterceptor
import org.grails.test.support.ControllerNameExtractor


import spock.lang.Specification
import spock.lang.Shared
import spock.lang.Stepwise
import org.grails.test.spock.GrailsSpecTestType
import org.springframework.context.ApplicationContext
import grails.util.Holders

/**
 * Super class for integration tests to extend
 *
 * @author Graeme Rocher
 * @author Luke Daley
 *
 * @since 2.3
 */
class IntegrationSpec extends Specification {
    @Shared private ApplicationContext applicationContext = Holders.getApplicationContext()
    @Shared private GrailsTestAutowirer autowirer = new GrailsTestAutowirer(applicationContext)

    @Shared private GrailsTestTransactionInterceptor perSpecTransactionInterceptor
    @Shared private GrailsTestRequestEnvironmentInterceptor perSpecRequestEnvironmentInterceptor

    private GrailsTestTransactionInterceptor perMethodTransactionInterceptor = null
    private GrailsTestRequestEnvironmentInterceptor perMethodRequestEnvironmentInterceptor = null

    def setupSpec() {
        if (isStepwiseSpec()) {
            perSpecTransactionInterceptor = initTransaction()
            perSpecRequestEnvironmentInterceptor = initRequestEnv()
        }

        autowirer.autowire(this)
    }

    def setup() {
        if (!isStepwiseSpec()) {
            perMethodTransactionInterceptor = initTransaction()
            perMethodRequestEnvironmentInterceptor = initRequestEnv()
        }

        autowirer.autowire(this)
    }

    def cleanup() {
        perMethodRequestEnvironmentInterceptor?.destroy()
        destroyTransaction(perMethodTransactionInterceptor)
    }

    def cleanupSpec() {
        perSpecRequestEnvironmentInterceptor?.destroy()
        destroyTransaction(perSpecTransactionInterceptor)
    }

    private boolean isStepwiseSpec() {
        getClass().isAnnotationPresent(Stepwise)
    }

    private GrailsTestTransactionInterceptor initTransaction() {
        def interceptor = new GrailsTestTransactionInterceptor(applicationContext)
        if (interceptor.isTransactional(this)) interceptor.init()
        interceptor
    }

    private void destroyTransaction(GrailsTestTransactionInterceptor interceptor){
        if (interceptor?.isTransactional(this)) interceptor.destroy()
    }

    private GrailsTestRequestEnvironmentInterceptor initRequestEnv() {
        def interceptor = new GrailsTestRequestEnvironmentInterceptor(applicationContext)
        def controllerName = ControllerNameExtractor.extractControllerNameFromTestClassName(
            this.class.name, GrailsSpecTestType.TEST_SUFFIXES as String[])
        interceptor.init(controllerName ?: GrailsTestRequestEnvironmentInterceptor.DEFAULT_CONTROLLER_NAME)
        interceptor
    }
}