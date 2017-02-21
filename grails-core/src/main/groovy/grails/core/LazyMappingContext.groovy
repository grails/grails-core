package grails.core

import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.validation.ValidatorRegistry
import org.springframework.validation.Validator

/**
 * Created by jameskleeh on 1/20/17.
 */
class LazyMappingContext {

    private MappingContext context
    private GrailsApplication grailsApplication

    LazyMappingContext() {

    }

    LazyMappingContext(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
    }

    GrailsDomainClass getDomainClass(String name) {
        if (grailsApplication != null) {
            (GrailsDomainClass)grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, name)
        } else {
            null
        }
    }

    void setMappingContext(MappingContext context) {
        this.context = context
    }

    boolean isInitialized() {
        this.context != null
    }

    PersistentEntity getPersistentEntity(String name) {
        context.getPersistentEntity(name)
    }

    Collection<PersistentEntity> getChildEntities(PersistentEntity root) {
        context.getChildEntities(root)
    }

    Validator getEntityValidator(PersistentEntity entity) {
        context.getEntityValidator(entity)
    }
}
