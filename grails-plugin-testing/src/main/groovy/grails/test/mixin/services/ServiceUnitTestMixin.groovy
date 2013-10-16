/*
 * Copyright 2011 SpringSource
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
package grails.test.mixin.services

import grails.test.mixin.domain.DomainClassUnitTestMixin
import org.codehaus.groovy.grails.commons.ServiceArtefactHandler

/**
 * A mixin that provides mocking capability for services.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class ServiceUnitTestMixin extends DomainClassUnitTestMixin {

    /**
     * Mocks a service class, registering it with the application context
     *
     * @param serviceClass The service class
     * @return An instance of the service
     */
    def <T> T testFor(Class<T> serviceClass) {
        return mockService(serviceClass)
    }

    /**
     * Mocks a service class, registering it with the application context
     *
     * @param serviceClass The service class
     * @return An instance of the service
     */
    def <T> T mockService(Class<T> serviceClass) {
        final serviceArtefact = grailsApplication.addArtefact(ServiceArtefactHandler.TYPE, serviceClass)

        defineBeans {
            "${serviceArtefact.propertyName}"(serviceClass) { bean ->
                bean.autowire = true
            }
        }

        applicationContext.getBean(serviceArtefact.propertyName, serviceClass)
    }
}
