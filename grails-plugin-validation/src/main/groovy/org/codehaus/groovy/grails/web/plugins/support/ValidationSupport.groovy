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

import grails.validation.ValidationErrors

import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.grails.datastore.gorm.support.BeforeValidateHelper
import org.springframework.validation.FieldError
import org.springframework.web.context.support.WebApplicationContextUtils

class ValidationSupport {

    static final beforeValidateHelper = new BeforeValidateHelper()

    static boolean validateInstance(object, List fieldsToValidate = null) {
        beforeValidateHelper.invokeBeforeValidate(object, fieldsToValidate)

        if (!object.hasProperty('constraints')) {
            return true
        }

        def constraints = object.constraints
        if (constraints) {
            def ctx

            def sch = ServletContextHolder.servletContext
            if (sch) {
                ctx = WebApplicationContextUtils.getWebApplicationContext(sch)
            }

            def messageSource = ctx?.containsBean('messageSource') ? ctx.getBean('messageSource') : null
            def localErrors = new ValidationErrors(object, object.class.name)
            def originalErrors = object.errors
            for (originalError in originalErrors.allErrors) {
                if (originalError instanceof FieldError) {
                    if (originalErrors.getFieldError(originalError.field)?.bindingFailure) {
                        localErrors.addError originalError
                    }
                } else {
                    localErrors.addError originalError
                }
            }
            for (prop in constraints.values()) {
                if (fieldsToValidate == null || fieldsToValidate.contains(prop.propertyName)) {
                    prop.messageSource = messageSource
                    prop.validate(object, object.getProperty(prop.propertyName), localErrors)
                }
            }
            object.errors = localErrors
        }

        return !object.errors.hasErrors()
    }
}
