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

import org.springframework.binding.expression.Expression
import org.springframework.binding.mapping.Mapper
import org.springframework.binding.mapping.Mapping
import org.springframework.binding.mapping.MappingResult
import org.springframework.binding.mapping.MappingResults
import org.springframework.binding.mapping.impl.DefaultMappingResults

/**
 * Mapper implementation for mapping subflow in- and output
 * @author Ivo Houbrechts
 */
abstract class AbstractMapper implements Mapper {
    private List mappings = []

    /**
     *
     * @param definition Closure in which method calls are interpreted as input or output variables
     * @return
     */
    AbstractMapper(Closure definition) {
        definition.delegate = this
        definition.resolveStrategy = Closure.DELEGATE_ONLY
        definition.call()
    }

    MappingResults map(Object source, Object target) {
        def results = []
        for (mapping in mappings) {
            results << map(source, target, mapping)
        }
        return new DefaultMappingResults(source, target, results)
    }

    protected abstract MappingResult map(source, target, mapping)

    /**
     * For each missing method a new mapping is created.
     * @param name
     * @param args
     * @return
     */
    def methodMissing(String name, args) {
        Object[] argArray = (Object[]) args
        boolean required = false
        def value = null
        if (argArray.length == 1) {
            if (argArray[0] instanceof Map) {
                required = argArray[0].required ?: false
                value = argArray[0].value
            }
            else {
                value = argArray[0]
            }
        }
        else if (argArray.length == 2 && argArray[0] instanceof Map && argArray[1] instanceof Closure) {
            required = argArray[0].required ?: false
            value = argArray[1]
        }
        else if (argArray.length != 0) {
            throw new RuntimeException("invalid arguments for input/output mapping $name: $argArray; expecting Map and/or Closure arguments")
        }
        mappings << createMapping(name, required, value)
    }

    private Mapping createMapping(String name, boolean required, def value) {
        KeyValueMapping mapping = new KeyValueMapping(key: name)
        mapping.required = required
        mapping.value = (value instanceof Closure) ? new ClosureExpression(value) : value
        return mapping
    }

}

/**
 * Mapping implementation that copies the value that corresponds with a key from a source Map to a target Map.
 */
class KeyValueMapping implements Mapping {
    String key
    boolean required
    def value

    Expression getSourceExpression() {
        new KeyExpression(key: key)
    }

    Expression getTargetExpression() {
        new KeyExpression(key: key)
    }

    def getValue(context) {
        (value instanceof Expression) ? value.getValue(context) : value
    }

    String toString() {"$key -> $key"}
}

/**
 * Simple Expression implementation that holds a Map key.
 */
class KeyExpression implements Expression {
    String key

    Object getValue(Object context) {
        context.get(key)
    }

    void setValue(Object context, Object value) {}

    Class getValueType(Object context) {Object}

    String getExpressionString() {key}
}
