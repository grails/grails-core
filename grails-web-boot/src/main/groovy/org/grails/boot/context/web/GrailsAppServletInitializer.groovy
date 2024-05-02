/*
 * Copyright 2024 original authors
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
package org.grails.boot.context.web

import grails.boot.GrailsAppBuilder
import groovy.transform.CompileStatic
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

/**
 * Ensure a {@link grails.boot.GrailsApp} in constructed during servlet initialization
 *
 * @author Graeme Rocher
 * @since 3.0.6
 */
@CompileStatic
abstract class GrailsAppServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder createSpringApplicationBuilder() {
        return new GrailsAppBuilder()
    }
}
