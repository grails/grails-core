/*
 * Copyright 2004-2005 the original author or authors.
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
package org.grails.plugins.domain

import org.springframework.context.ApplicationContext
import org.springframework.validation.BeanPropertyBindingResult
import grails.validation.ValidationErrors

/**
 * @author Graeme Rocher
 * @since 1.1
 *
 * @deprecated Will be removed in a future version of Grails
 */
@Deprecated
class DomainClassPluginSupport {

    /**
     * Validates a domain class (or command object) instance.
     */
    static validateInstance(object, ApplicationContext ctx) {
        def localErrors = new ValidationErrors(object)
        if (!object.hasProperty('constraints')) {
            return true
        }

        def constraints = object.getConstraints()
        if (constraints) {
            for (prop in constraints.values()) {
                prop.messageSource = ctx.getBean("messageSource")
                prop.validate(object, object.getProperty(prop.getPropertyName()), localErrors)
            }
            if (localErrors.hasErrors()) {
                def objectErrors = object.errors
                localErrors.allErrors.each { localError ->
                    def fieldName = localError.getField()
                    def fieldError = objectErrors.getFieldError(fieldName)

                    // if we didn't find an error OR if it is a bindingFailure...
                    if (!fieldError || fieldError.bindingFailure) {
                        objectErrors.addError(localError)
                    }
                }
            }
        }

        return !object.errors.hasErrors()
    }
}
