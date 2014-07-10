/*
 * Copyright 2013 SpringSource
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
package org.grails.test.runner.phase

import groovy.transform.CompileStatic

/**
 * @author Graeme Rocher
 */
@CompileStatic
class DefaultTestPhaseConfigurer implements TestPhaseConfigurer{


    @Override
    void prepare(Binding testExecutionContext, Map<String, Object> testOptions) {
        // noop, subclasses can override
    }

    @Override
    void cleanup(Binding testExecutionContext, Map<String, Object> testOptions) {
        // noop, subclasses can override
    }
}
