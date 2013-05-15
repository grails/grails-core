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
package org.codehaus.groovy.grails.plugins.web.mimes

import org.apache.commons.collections.map.ListOrderedMap

/**
 * Interceptors a closure call and gathers method calls that take a closure into a map format->closure
 *
 * @author Graeme Rocher
 * @since 1.2
 */
class FormatInterceptor {
    def formatOptions = new ListOrderedMap()
    Object invokeMethod(String name,args) {
        if (args.size() > 0 && (args[0] instanceof Closure || args[0] instanceof Map)) {
            formatOptions[name] = args[0]
        }
        else {
            formatOptions[name] = null
        }
    }
}
