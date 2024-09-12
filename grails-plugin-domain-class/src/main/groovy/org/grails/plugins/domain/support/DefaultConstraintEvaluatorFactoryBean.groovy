package org.grails.plugins.domain.support

import grails.core.GrailsApplication
import grails.util.GrailsMessageSource
import org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator
import org.grails.datastore.gorm.validation.constraints.eval.DefaultConstraintEvaluator
import org.grails.datastore.gorm.validation.constraints.registry.ConstraintRegistry
import org.grails.datastore.gorm.validation.constraints.registry.DefaultConstraintRegistry
import org.grails.datastore.mapping.model.MappingContext
import org.grails.validation.ConstraintEvalUtils
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource

class DefaultConstraintEvaluatorFactoryBean implements FactoryBean<ConstraintsEvaluator> {

    MessageSource messageSource

    @Autowired
    setMessageSource(List<MessageSource> messageSources) {
        messageSource = GrailsMessageSource.getMessageSource(messageSources)
    }

    void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource
    }

    @Autowired
    @Qualifier('grailsDomainClassMappingContext')
    MappingContext grailsDomainClassMappingContext

    @Autowired
    GrailsApplication grailsApplication

    @Override
    ConstraintsEvaluator getObject() throws Exception {
        ConstraintRegistry registry = new DefaultConstraintRegistry(messageSource)

        new DefaultConstraintEvaluator(registry, grailsDomainClassMappingContext, ConstraintEvalUtils.getDefaultConstraints(grailsApplication.config))
    }

    @Override
    Class<?> getObjectType() {
        ConstraintsEvaluator
    }

    @Override
    boolean isSingleton() {
        true
    }
}
