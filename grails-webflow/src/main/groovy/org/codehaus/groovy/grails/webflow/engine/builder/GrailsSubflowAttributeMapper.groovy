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
 */
package org.codehaus.groovy.grails.webflow.engine.builder

import org.springframework.webflow.core.collection.AttributeMap
import org.springframework.webflow.core.collection.LocalAttributeMap
import org.springframework.webflow.core.collection.MutableAttributeMap
import org.springframework.webflow.engine.SubflowAttributeMapper
import org.springframework.webflow.execution.RequestContext

/**
 * SubflowAttributeMapper implementation for mapping subflow in- and outputs.
 * @author Ivo Houbrechts
 */
class GrailsSubflowAttributeMapper implements SubflowAttributeMapper {
    private Map subflowInput

    GrailsSubflowAttributeMapper(Map input) {
        this.subflowInput = input
    }

    MutableAttributeMap createSubflowInput(RequestContext context) {
        LocalAttributeMap result = new LocalAttributeMap()
        subflowInput.each {key, value ->
            if (value instanceof ClosureExpression) {
                result.put(key, value.getValue(context))
            }
            else {
                result.put(key, value)
            }
        }
        return result
    }

    void mapSubflowOutput(AttributeMap output, RequestContext context) {}
}
