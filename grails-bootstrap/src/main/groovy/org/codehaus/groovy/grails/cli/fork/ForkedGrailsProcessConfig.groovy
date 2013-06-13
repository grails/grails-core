/*
 * Copyright 2012 SpringSource
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
package org.codehaus.groovy.grails.cli.fork

import groovy.transform.CompileStatic

/**
 * Configuration for a forked process
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class ForkedGrailsProcessConfig {

    ForkedGrailsProcessConfig(config) {
        if (config instanceof Map<String, Object>) {
            forked = true
            def map = (Map<String, Object>)config

            if (map.maxMemory) {
                maxMemory = map.maxMemory.toString().toInteger()
            }
            if (map.minMemory) {
                minMemory = map.minMemory.toString().toInteger()
            }
            if (map.maxPerm) {
                maxPerm = map.maxPerm.toString().toInteger()
            }
            if (map.debug) {
                debug = true
            }
        }
        else if (config) {
            forked = true
        }
    }

    ForkedGrailsProcessConfig() {
    }

    Integer maxMemory = 512
    Integer minMemory = 64
    boolean debug = false
    Integer maxPerm = 256
    boolean forked = false

    Map asMap() {
        [maxMemory:maxMemory, minMemory:minMemory, debug:debug, maxPerm:maxPerm]
    }
}
