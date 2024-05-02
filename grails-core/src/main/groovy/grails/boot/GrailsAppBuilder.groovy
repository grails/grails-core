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
package grails.boot

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.springframework.boot.SpringApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.core.io.ResourceLoader

/**
 * Fluent API for constructing GrailsApp instances. Simple extension of {@link SpringApplicationBuilder}.
 *
 * @author Graeme Rocher
 * @since 3.0.6
 */
@CompileStatic
@InheritConstructors
class GrailsAppBuilder extends SpringApplicationBuilder {

    @Override
    protected SpringApplication createSpringApplication(ResourceLoader resourceLoader, Class < ? > ... sources) {
        return new GrailsApp(resourceLoader, sources)
    }
}
