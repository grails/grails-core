package org.grails.core.support

import grails.core.GrailsApplication
import grails.gorm.validation.PersistentEntityValidator
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
 * Created by jameskleeh on 2/2/17.
 */
class MappingContextBuilder {

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
