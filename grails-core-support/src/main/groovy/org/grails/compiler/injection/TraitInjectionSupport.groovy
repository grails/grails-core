/*
 * Copyright 2015 original authors
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

package org.grails.compiler.injection

import grails.compiler.traits.TraitInjector
import groovy.transform.CompileStatic


/**
 * Utility methods used by {@link TraitInjectionUtils}
 *
 * @author Graeme Rocher
 * @since 3.0.3
 */
@CompileStatic
class TraitInjectionSupport {

    static List<TraitInjector> resolveTraitInjectors(List<TraitInjector> injectors) {
        injectors = new ArrayList<>(injectors)
        injectors.sort { TraitInjector o1, TraitInjector o2 ->
            final Class t1 = o1.trait
            final Class t2 = o2.trait
            if(t1 == t2) return 0

            // lower priority of core traits so that plugins can override
            if(o1.getClass().name.startsWith('grails.compiler.traits')) {
                return -1
            }
            else {
                return 1
            }
        }
        return injectors
    }
}
