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
package grails.validation.trait

import grails.util.GrailsNameUtils
import grails.util.Holders
import grails.validation.ValidationErrors

import java.lang.reflect.Method
import java.lang.reflect.Modifier

import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.codehaus.groovy.grails.validation.ConstraintsEvaluator
import org.grails.datastore.gorm.support.BeforeValidateHelper
import org.springframework.beans.factory.BeanFactory
import org.springframework.validation.Errors
import org.springframework.validation.FieldError
import org.springframework.web.context.support.WebApplicationContextUtils

/**
 * @since 3.0
 * @author Jeff Brown
 */
trait Validateable {
    private static BeforeValidateHelper beforeValidateHelper = new BeforeValidateHelper()
    private static Map constraintsMapInternal
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

    private void initErrors()  {
        if (errors == null) {
            errors = new ValidationErrors(this, this.getClass().getName())
        }
    }

    static Map getConstraintsMap() {
        if(this.constraintsMapInternal == null) {
            BeanFactory ctx = Holders.applicationContext
            ConstraintsEvaluator evaluator = ctx.getBean(ConstraintsEvaluator.BEAN_NAME, ConstraintsEvaluator)
            this.constraintsMapInternal = evaluator.evaluate this, defaultNullable()

            if(!defaultNullable()) {
                def methods = this.getDeclaredMethods()
                for(Method method : methods) {
                    if(!Modifier.isStatic(method.modifiers) && !method.parameterTypes) {
                        def methodName = method.name
                        if(methodName ==~ /get[A-Z].*/) {
                            def propertyName = GrailsNameUtils.getPropertyName(methodName[3..-1])
                            if(propertyName != 'metaClass' &&
                            propertyName != 'errors' &&
                            !this.constraintsMapInternal.containsKey(propertyName)) {
                                def cp = new ConstrainedProperty(this, propertyName, method.returnType)
                                cp.applyConstraint 'nullable', false
                                this.constraintsMapInternal.put propertyName, cp
                            }
                        }
                    }
                }
            }
        }
        this.constraintsMapInternal
    }

    boolean validate() {
        validate((List)null)
    }
    
    boolean validate(List fieldsToValidate) {
        beforeValidateHelper.invokeBeforeValidate(this, fieldsToValidate)

        def constraints = getConstraintsMap()
        if (constraints) {
            def ctx

            def sch = Holders.servletContext
            if (sch) {
                ctx = WebApplicationContextUtils.getWebApplicationContext(sch)
            }

            def messageSource = ctx?.containsBean('messageSource') ? ctx.getBean('messageSource') : null
            def localErrors = new ValidationErrors(this, this.class.name)
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
                    if(fieldError == null || !fieldError.bindingFailure) {
                        prop.messageSource = messageSource
                        prop.validate(this, this.getProperty(prop.propertyName), localErrors)
                    }
                }
            }
            errors = localErrors
        }

        return !errors.hasErrors()
    }

    static boolean defaultNullable() {
        false
    }
}
