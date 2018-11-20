package org.grails.web.plugins.support

import grails.gorm.validation.ConstrainedEntity
import grails.gorm.validation.ConstrainedProperty
import grails.validation.Constrained
import grails.validation.ConstrainedDelegate
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.validation.discovery.ConstrainedDiscovery
import org.springframework.validation.Validator

/**
 * Discovers the default constrained properties for a domain class
 *
 * @author Graeme Rocher
 * @since 3.3
 */
@CompileStatic
class DefaultConstrainedDiscovery implements ConstrainedDiscovery {
    @Override
    Map<String, Constrained> findConstrainedProperties(PersistentEntity entity) {
        Validator validator = entity.getMappingContext().getEntityValidator(entity)
        if(validator instanceof ConstrainedEntity) {
            ConstrainedEntity constrainedEntity = (ConstrainedEntity)validator
            Map<String, ConstrainedProperty> constrainedProperties = constrainedEntity.getConstrainedProperties()
            return adaptConstraints(constrainedProperties)
        }
        return Collections.emptyMap()
    }

    private static Map<String, Constrained> adaptConstraints(Map<String, ConstrainedProperty> evaluated) {
        Map<String, Constrained> finalConstraints = new LinkedHashMap<>(evaluated.size());
        for (Map.Entry<String, ConstrainedProperty> entry : evaluated.entrySet()) {
            finalConstraints.put(entry.getKey(), new ConstrainedDelegate(entry.getValue()));
        }
        return finalConstraints;
    }

}
