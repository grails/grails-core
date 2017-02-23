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
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.support.BeforeValidateHelper
import org.grails.datastore.gorm.validation.constraints.eval.DefaultConstraintEvaluator
import org.grails.datastore.gorm.validation.constraints.registry.DefaultConstraintRegistry
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.validation.ConstrainedDelegate
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import org.springframework.context.support.StaticMessageSource
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
@CompileStatic
trait Validateable {
    private BeforeValidateHelper beforeValidateHelper = new BeforeValidateHelper()
    private static Map<String, Constrained> constraintsMapInternal
    Errors errors

    /**
     * @return The errors
     */
    Errors getErrors() {
        initErrors()
        errors
    }

    /**
     * @return Whether the object has errors
     */
    Boolean hasErrors() {
        initErrors()
        errors.hasErrors()
    }

    /**
     * Clear the errors
     */
    void clearErrors() {
        errors = null
    }

    /**
     * @return The map of applied constraints
     */
    static Map<String, Constrained> getConstraintsMap() {
        if (constraintsMapInternal == null) {
            org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator evaluator = findConstraintsEvaluator()
            Map<String, grails.gorm.validation.ConstrainedProperty> evaluatedConstraints = evaluator.evaluate(this, defaultNullable())

            Map<String, Constrained> finalConstraints = [:]
            for(entry in evaluatedConstraints) {
                finalConstraints.put(entry.key, new ConstrainedDelegate(entry.value))
            }

            constraintsMapInternal = finalConstraints
        }
        return constraintsMapInternal
    }

    /**
     * Validate the object
     *
     * @return True if it is valid
     */
    boolean validate() {
        validate null, null, null
    }

    /**
     * Validate the object with the given adhoc constraints
     *
     * @return True if it is valid
     */
    boolean validate(Closure<?>... adHocConstraintsClosures) {
        validate(null, null, adHocConstraintsClosures)
    }

    /**
     * Validate the object with the given parameters
     *
     * @return True if it is valid
     */
    boolean validate(Map<String, Object> params) {
        validate params, null
    }

    /**
     * Validate the object with the given parameters and adhoc constraints
     *
     * @return True if it is valid
     */
    boolean validate(Map<String, Object> params, Closure<?>... adHocConstraintsClosures) {
        validate(null, params, adHocConstraintsClosures)
    }

    /**
     * Validate the object for the given list of fields
     *
     * @return True if it is valid
     */
    boolean validate(List fieldsToValidate) {
        validate fieldsToValidate, null, null
    }

    /**
     * Validate the object for the given list of fields and adhoc constraints
     *
     * @return True if it is valid
     */
    boolean validate(List fieldsToValidate, Closure<?>... adHocConstraintsClosures) {
        validate(fieldsToValidate, null, adHocConstraintsClosures)
    }

    /**
     * Validate the object for the given list of fields and parameters
     *
     * @return True if it is valid
     */
    boolean validate(List fieldsToValidate, Map<String, Object> params) {
        validate fieldsToValidate, params, null
    }

    /**
     * Validate the object for the given list of fields, parameters and adhoc constraints
     *
     * @return True if it is valid
     */
    boolean validate(List fieldsToValidate, Map<String, Object> params, Closure<?>... adHocConstraintsClosures) {
        beforeValidateHelper.invokeBeforeValidate(this, fieldsToValidate)

        boolean shouldInherit = Boolean.valueOf(params?.inherit?.toString() ?: 'true')
        org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator evaluator = findConstraintsEvaluator()

        Map<String, grails.gorm.validation.ConstrainedProperty> constraints = evaluator.evaluate(this.class, defaultNullable(), !shouldInherit, adHocConstraintsClosures)

        ValidationErrors localErrors = doValidate(constraints, fieldsToValidate)

        boolean clearErrors = Boolean.valueOf(params?.clearErrors?.toString() ?: 'true')
        if (errors && !clearErrors) {
            errors.addAllErrors(localErrors)
        } else {
            errors = localErrors
        }
        return !errors.hasErrors()
    }

    private ValidationErrors doValidate(Map<String, grails.gorm.validation.ConstrainedProperty> constraints, List fieldsToValidate) {
        ValidationErrors localErrors = new ValidationErrors(this, this.class.name)
        if (constraints) {
            Errors originalErrors = getErrors()
            for (originalError in originalErrors.allErrors) {
                if (originalError instanceof FieldError) {
                    if (originalErrors.getFieldError(((FieldError)originalError).field)?.bindingFailure) {
                        localErrors.addError originalError
                    }
                } else {
                    localErrors.addError originalError
                }
            }
            for (prop in constraints.values()) {
                if (fieldsToValidate == null || fieldsToValidate.contains(prop.propertyName)) {
                    FieldError fieldError = originalErrors.getFieldError(prop.propertyName)
                    if (fieldError == null || !fieldError.bindingFailure) {
                        def value = getPropertyValue(prop)
                        prop.validate(this, value, localErrors)
                    }
                }
            }
        }
        localErrors
    }

    @CompileDynamic
    private Object getPropertyValue(grails.gorm.validation.ConstrainedProperty prop) {
        this.getProperty(prop.propertyName)
    }

    @CompileStatic
    private static org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator findConstraintsEvaluator() {
        try {
            ApplicationContext ctx = Holders.applicationContext
            org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator evaluator = ctx.getBean(org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator)
            return evaluator
        } catch (Throwable e) {
            MessageSource messageSource = Holders.findApplicationContext() ?: new StaticMessageSource()
            return new DefaultConstraintEvaluator(
                    new DefaultConstraintRegistry(messageSource),
                    new KeyValueMappingContext(""),
                    Collections.emptyMap()
            )
        }
    }

    private MessageSource findMessageSource() {
        try {
            ApplicationContext ctx = Holders.findApplicationContext()
            MessageSource messageSource = ctx?.containsBean('messageSource') ? ctx.getBean('messageSource', MessageSource) : null
            return messageSource
        } catch (Throwable e) {
            return null
        }
    }

    private void initErrors() {
        if (errors == null) {
            errors = new ValidationErrors(this, this.getClass().getName())
        }
    }

    static boolean defaultNullable() {
        false
    }
}
