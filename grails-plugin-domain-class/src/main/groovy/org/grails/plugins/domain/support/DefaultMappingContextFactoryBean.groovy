package org.grails.plugins.domain.support

import grails.core.GrailsApplication
import grails.core.GrailsClass
import groovy.transform.CompileStatic
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.datastore.gorm.validation.constraints.factory.ConstraintFactory
import org.grails.datastore.gorm.validation.constraints.registry.DefaultValidatorRegistry
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettings
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettingsBuilder
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.model.MappingContext
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.MessageSource
import org.springframework.core.env.PropertyResolver

/**
 * A factory bean for creating the default mapping context where an implementation of GORM is not present
 *
 * @author Graeme Rocher
 * @since 3.3
 */
@CompileStatic
class DefaultMappingContextFactoryBean implements FactoryBean<MappingContext>, InitializingBean {

    final PropertyResolver configuration
    final GrailsApplication grailsApplication
    protected final MessageSource messageSource
    protected final ApplicationContext applicationContext
    private MappingContext mappingContext

    DefaultMappingContextFactoryBean(GrailsApplication grailsApplication, MessageSource messageSource) {
        this.configuration = grailsApplication.config
        this.messageSource = messageSource
        this.grailsApplication = grailsApplication
        if(messageSource instanceof ApplicationContext) {
            this.applicationContext = (ApplicationContext)messageSource
        }
        else {
            applicationContext = null
        }
    }


    @Override
    MappingContext getObject() throws Exception {
        return mappingContext
    }

    @Override
    Class<?> getObjectType() {
        return MappingContext.class
    }

    @Override
    boolean isSingleton() {
        return true
    }

    @Autowired(required = false)
    List<ConstraintFactory> constraintFactories = []

    @Override
    void afterPropertiesSet() throws Exception {
        ConnectionSourceSettingsBuilder builder = new ConnectionSourceSettingsBuilder(configuration)
        ConnectionSourceSettings settings = builder.build()

        this.mappingContext = new KeyValueMappingContext("default", settings)
        DefaultValidatorRegistry validatorRegistry = new DefaultValidatorRegistry(mappingContext, settings, messageSource)
        for(factory in constraintFactories) {
            validatorRegistry.addConstraintFactory(factory)
        }
        mappingContext.setValidatorRegistry(
                validatorRegistry
        )

        GrailsClass[] persistentClasses = grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE)
        mappingContext.addPersistentEntities(persistentClasses.collect() { GrailsClass cls -> cls.clazz} as Class[])
    }
}
