/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.grails.datastore.gorm.validation.jakarta

import groovy.transform.CompileStatic

import jakarta.validation.Validator

import org.springframework.context.MessageSource
import org.springframework.validation.Errors

import grails.gorm.validation.ConstrainedProperty
import grails.gorm.validation.PersistentEntityValidator
import org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator
import org.grails.datastore.gorm.validation.constraints.registry.ConstraintRegistry
import org.grails.datastore.mapping.model.PersistentEntity

/**
 * Validates a persistent entity annotated with Jakarta Bean Validation constraints: its GORM constraints (the
 * {@code constraints} block and the defaults) as a {@link PersistentEntityValidator}, then its annotations with the
 * Bean Validation provider, whose violations are reported as field errors with Spring's Bean Validation message
 * codes (such as {@code NotNull.member.name}, {@code NotNull.name} and {@code NotNull}).
 * <p>
 * The GORM {@code nullable} constraint is left to Bean Validation for a property whose nullability an annotation
 * of the default group declares ({@code @NotNull}, {@code @NotBlank}, {@code @NotEmpty} or {@code @Null}), so a
 * null value is reported once. Bean Validation does not cascade: the GORM validator cascades to the associations
 * as it does for any entity, and validates an annotated associated entity both ways through
 * {@link #validateBeyondConstraints}.
 * <p>
 * {@link #getConstrainedProperties()} describes the annotations as GORM constraints next to the ones of the block,
 * see {@link JakartaConstraintsTranslator}.
 *
 * @since 8.0.1
 */
@CompileStatic
class JakartaPersistentEntityValidator extends PersistentEntityValidator {

    /**
     * The validator for the Bean Validation annotations of the entity
     */
    final GormValidatorAdapter beanValidator

    private final Map<String, ConstrainedProperty> describedConstrainedProperties

    /**
     * @param entity The entity
     * @param messageSource The message source
     * @param constraintsEvaluator The evaluator of the GORM constraints
     * @param constraintRegistry The registry the constraints describing the annotations are built with
     * @param beanValidator The Bean Validation validator
     */
    JakartaPersistentEntityValidator(PersistentEntity entity, MessageSource messageSource, ConstraintsEvaluator constraintsEvaluator,
            ConstraintRegistry constraintRegistry, Validator beanValidator) {
        super(entity, messageSource, constraintsEvaluator)
        this.beanValidator = beanValidator instanceof GormValidatorAdapter
                ? (GormValidatorAdapter) beanValidator
                : new GormValidatorAdapter(beanValidator)
        JakartaConstraintsTranslator translator = new JakartaConstraintsTranslator(this.beanValidator.getConstraintsForClass(targetClass))
        Map<String, ConstrainedProperty> validated = getValidatedConstrainedProperties()
        translator.delegateNullabilityToBeanValidation(validated)
        this.describedConstrainedProperties = Collections.unmodifiableMap(translator.translate(validated, constraintRegistry))
    }

    /**
     * @return The constraints of the entity, the Bean Validation annotations described as GORM constraints next to
     * the ones of the {@code constraints} block. The annotations themselves are validated by Bean Validation, see
     * {@link #getValidatedConstrainedProperties()} for the constraints this validator validates.
     */
    @Override
    Map<String, ConstrainedProperty> getConstrainedProperties() {
        return describedConstrainedProperties
    }

    /**
     * Validates the Bean Validation annotations of the object, without cascading, as the GORM validator cascades.
     */
    @Override
    protected void validateBeyondConstraints(Object obj, Errors errors) {
        beanValidator.validate(obj, errors, false)
    }
}
