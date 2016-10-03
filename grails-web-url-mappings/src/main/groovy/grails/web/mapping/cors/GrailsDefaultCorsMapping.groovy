/*
 * Copyright 2014 original authors
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
package grails.web.mapping.cors

import groovy.transform.CompileStatic
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.cors.CorsConfiguration
import static org.springframework.web.bind.annotation.CrossOrigin.*
/**
 * A class that stores default CORS settings
 *
 * @author James Kleeh
 * @since 3.2.1
 */
@CompileStatic
class GrailsDefaultCorsMapping {

    List<String> allowedOrigins = DEFAULT_ORIGINS.toList()
    List<String> allowedMethods = ["*"]
    List<String> allowedHeaders = DEFAULT_ALLOWED_HEADERS.toList()
    List<String> exposedHeaders = null
    Long maxAge = DEFAULT_MAX_AGE
    Boolean allowCredentials = DEFAULT_ALLOW_CREDENTIALS

    CorsConfiguration toSpringConfig() {
        CorsConfiguration defaultCorsConfiguration = new CorsConfiguration()
        defaultCorsConfiguration.allowedOrigins = allowedOrigins
        defaultCorsConfiguration.allowedMethods = allowedMethods
        defaultCorsConfiguration.allowedHeaders = allowedHeaders
        defaultCorsConfiguration.exposedHeaders = exposedHeaders
        defaultCorsConfiguration.maxAge = maxAge
        defaultCorsConfiguration.allowCredentials = allowCredentials
        defaultCorsConfiguration
    }
}
