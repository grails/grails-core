/* Copyright 2004-2005 Graeme Rocher
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

/**
 * Implements the Spring binding expression interface to allow an expression to evaluate
 * a closure for using with dynamic transitions.
 *
 * @author Graeme Rocher
 * @since 0.6
 */
class ClosureExpression implements Expression {

    Closure closure

    ClosureExpression(Closure c) {
        this.closure = c
    }

    Object getValue(Object context) {
        def attrs = context?.attributes ? context.attributes : [:]
        closure.delegate = new ControllerDelegate(context)
        closure.resolveStrategy = Closure.DELEGATE_ONLY
        closure.call()
    }

    void setValue(Object context, Object value) {
        // do nothing
    }

    Class getValueType(Object context) { Object }

    String getExpressionString() {
        c.inspect()
    }
}
