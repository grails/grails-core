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
package org.grails.core.support

import grails.core.GrailsApplication
import org.springframework.context.ApplicationContext

/**
 * Interface used for classes that discover the GrailsApplication and ApplicationContext instances
 *
 * @since 2.4
 */
public interface GrailsApplicationDiscoveryStrategy {

    /**
     * @return Find the GrailsApplication instance
     *
     * @throws IllegalStateException If the application could not be found due to misconfiguration
     */
    GrailsApplication findGrailsApplication()

    /**
     * @return Find the ApplicationContext instance
     *
     * @throws IllegalStateException If the application could not be found due to misconfiguration
     */
    ApplicationContext findApplicationContext()

}