/* Copyright 2004-2005 Ivo Houbrechts
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
 */ package org.codehaus.groovy.grails.webflow.engine.builder

import org.springframework.binding.mapping.MappingResult
import org.springframework.binding.mapping.results.Success

/**
 * Mapper implementation that copies subflow output values.
 * @author Ivo Houbrechts
 */
class OutputMapper extends AbstractMapper {

    OutputMapper(Closure definition) {
        super(definition)
    }

    protected MappingResult map(source, target, mapping) {
        def mappedValue = mapping.getValue(source)
        target.put(mapping.key, mappedValue)
        return new Success(mapping, mappedValue, null)
    }
}
