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
package org.codehaus.groovy.grails.web.plugins.support

import org.codehaus.groovy.grails.validation.ConstraintsEvaluator
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.web.context.ContextLoader


class ValidationSupport {

    static validateInstance(object, List fieldsToValidate = null) {
        if (!object.hasProperty('constrainedProperties')) {
            return true
        }
        def ctx = ContextLoader.currentWebApplicationContext

        def constraints = object.constrainedProperties
        if(constraints == null) {
            def constraintsEvaluator = ctx?.containsBean(ConstraintsEvaluator.BEAN_NAME) ? ctx.getBean(ConstraintsEvaluator.BEAN_NAME) : new DefaultConstraintEvaluator()
            constraints = object.constrainedProperties = constraintsEvaluator.evaluate(object)
        }
        if (constraints) {
            def messageSource = ctx?.containsBean('messageSource') ? ctx.getBean('messageSource') : null
            def localErrors = new BeanPropertyBindingResult(object, object.class.name)
            def originalErrors = object.errors
            for(originalError in originalErrors.allErrors) {
                if(originalErrors.getFieldError(originalError.field)?.bindingFailure) {
                    localErrors.rejectValue originalError.field, originalError.code, originalError.arguments, originalError.defaultMessage
                }
            }
            for (prop in constraints.values()) {
                if(fieldsToValidate == null || fieldsToValidate.contains(prop.propertyName)) {
                    prop.messageSource = messageSource
                    prop.validate(object, object.getProperty(prop.propertyName), localErrors)
                }
            }
            object.errors = localErrors
        }

        return !object.errors.hasErrors()
    }
}
