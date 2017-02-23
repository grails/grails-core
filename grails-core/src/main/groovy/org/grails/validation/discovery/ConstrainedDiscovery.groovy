package org.grails.validation.discovery

import grails.validation.Constrained
import org.grails.datastore.mapping.model.PersistentEntity

/**
 * Strategy interface for discovering the {@link grails.validation.Constrained} properties of a class
 *
 * @author Graeme Rocher
 * @since 6.1
 */
interface ConstrainedDiscovery {

    /**
     * Finds the constrained properties for the given entity
     *
     * @param entity The entity
     * @return The constrained properties
     */
    Map<String, Constrained> findConstrainedProperties(PersistentEntity entity)
}