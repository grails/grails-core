/*
 * Copyright 2024 original authors
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

import grails.core.GrailsApplication
import grails.gorm.validation.PersistentEntityValidator
import grails.plugins.GrailsPlugin
import org.grails.datastore.gorm.validation.constraints.eval.DefaultConstraintEvaluator
import org.grails.datastore.gorm.validation.constraints.registry.DefaultConstraintRegistry
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.springframework.context.MessageSource
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.StaticMessageSource
import org.springframework.validation.Validator

/**
 * Created by graemerocher on 21/02/2017.
 */
class MappingContextBuilder {
    public static GrailsPlugin FAKE_HIBERNATE_PLUGIN = [getName: { -> 'hibernate' }] as GrailsPlugin
    private GrailsApplication grailsApplication
    private MappingContext mappingContext
    private DefaultConstraintEvaluator constraintEvaluator
    private MessageSource messageSource

    MappingContextBuilder(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
        this.mappingContext = new KeyValueMappingContext(UUID.randomUUID().toString())
        this.messageSource = new StaticMessageSource()
        DefaultConstraintRegistry constraintRegistry = new DefaultConstraintRegistry(messageSource)
        constraintEvaluator = new DefaultConstraintEvaluator(constraintRegistry, mappingContext, [:])
    }

    void build(Class...classes) {
        Collection<PersistentEntity> entities = mappingContext.addPersistentEntities(classes)
        for (PersistentEntity entity in entities) {
            entity.initialize()
            Validator validator = new PersistentEntityValidator(entity, messageSource, constraintEvaluator)
            mappingContext.addEntityValidator(entity, validator)
        }
        GenericApplicationContext context
        if (grailsApplication.parentContext instanceof GenericApplicationContext) {
            context = grailsApplication.parentContext
        } else {
            context = new GenericApplicationContext()
            context.refresh()
        }
        context.beanFactory.registerSingleton("grailsDomainClassMappingContext", mappingContext)
        grailsApplication.setApplicationContext(context)
        grailsApplication.setMappingContext(mappingContext)
    }
}
