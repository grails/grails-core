/*
 * Copyright 2014 the original author or authors.
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
package grails.validation

import grails.util.Holders
import grails.validation.ConstrainedProperty
import grails.validation.ConstraintsEvaluator
import grails.validation.ValidationErrors
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.support.BeforeValidateHelper
import org.grails.validation.DefaultConstraintEvaluator
import org.springframework.beans.factory.BeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import org.springframework.validation.Errors
import org.springframework.validation.FieldError

/**
 * A trait that can be applied to make any object Validateable
 *
 * @since 3.0
 *
 * @author Jeff Brown
 * @author Graeme Rocher
 */
trait Validateable {
    private BeforeValidateHelper beforeValidateHelper = new BeforeValidateHelper()
    private static Map<String, ConstrainedProperty> constraintsMapInternal
    Errors errors

    Errors getErrors() {
        initErrors()
        errors
    }

    Boolean hasErrors() {
        initErrors()
        errors.hasErrors()
    }

    void clearErrors() {
        errors = null
    }

    private void initErrors() {
        if (errors == null) {
            errors = new ValidationErrors(this, this.getClass().getName())
        }
    }

    static Map<String, ConstrainedProperty> getConstraintsMap() {
        if (constraintsMapInternal == null) {
            ConstraintsEvaluator evaluator = findConstraintsEvaluator()
            def evaluatedConstraints = evaluator.evaluate(this, defaultNullable())
            constraintsMapInternal = evaluatedConstraints
        }
        constraintsMapInternal
    }

    @CompileStatic
    private static ConstraintsEvaluator findConstraintsEvaluator() {
        try {
            BeanFactory ctx = Holders.applicationContext
            ConstraintsEvaluator evaluator = ctx.getBean(ConstraintsEvaluator.BEAN_NAME, ConstraintsEvaluator)
            return evaluator
        } catch (Throwable e) {
            return new DefaultConstraintEvaluator()
        }
    }

    boolean validate() {
        validate(null)
    }

    boolean validate(List fieldsToValidate) {
        beforeValidateHelper.invokeBeforeValidate(this, fieldsToValidate)

        //initialize errors before constraints block, because this block may be empty
        def localErrors = new ValidationErrors(this, this.class.name)

        def constraints = getConstraintsMap()
        if (constraints) {
            Object messageSource = findMessageSource()
            def originalErrors = getErrors()
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
                    def fieldError = originalErrors.getFieldError(prop.propertyName)
                    if (fieldError == null || !fieldError.bindingFailure) {
                        prop.messageSource = messageSource

                        def value = getPropertyValue(prop)
                        prop.validate(this, value, localErrors)
                    }
                }
            }
        }

        //set errors instance even if constraints were not verified
        //object should must have it initialized after validate()
        errors = localErrors
        return !errors.hasErrors()
    }

    private Object getPropertyValue(ConstrainedProperty prop) {
        this.getProperty(prop.propertyName)
    }

    private MessageSource findMessageSource() {
        try {
            ApplicationContext ctx = Holders.applicationContext
            MessageSource messageSource = ctx?.containsBean('messageSource') ? ctx.getBean('messageSource', MessageSource) : null
            return messageSource
        } catch (Throwable e) {
            return null
        }
    }

    static boolean defaultNullable() {
        false
    }
}
