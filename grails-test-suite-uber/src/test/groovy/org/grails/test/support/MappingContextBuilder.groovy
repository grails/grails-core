package org.grails.test.support

import grails.core.GrailsApplication
import grails.core.GrailsDomainClass
import grails.gorm.validation.PersistentEntityValidator
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.datastore.gorm.validation.constraints.eval.DefaultConstraintEvaluator
import org.grails.datastore.gorm.validation.constraints.registry.DefaultConstraintRegistry
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.support.MockApplicationContext
import org.grails.validation.ConstraintEvalUtils
import org.springframework.context.MessageSource
import org.springframework.context.MessageSourceResolvable
import org.springframework.context.NoSuchMessageException
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
        MockApplicationContext context
        if (grailsApplication.parentContext instanceof MockApplicationContext) {
            context = grailsApplication.parentContext
        } else {
            context = new MockApplicationContext()
        }
        context.registerMockBean("grailsDomainClassMappingContext", mappingContext)
        grailsApplication.setApplicationContext(context)
    }
}
