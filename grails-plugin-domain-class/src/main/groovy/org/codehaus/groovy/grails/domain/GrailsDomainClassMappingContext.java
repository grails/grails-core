/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.domain;

import java.util.List;
import java.util.Set;

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.grails.datastore.mapping.model.AbstractMappingContext;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.IdentityMapping;
import org.grails.datastore.mapping.model.MappingConfigurationStrategy;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.MappingFactory;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.PropertyMapping;
import org.grails.datastore.mapping.model.types.Identity;

/**
 * A MappingContext that adapts the Grails domain model to the Mapping API.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class GrailsDomainClassMappingContext extends AbstractMappingContext {

    private GrailsApplication grailsApplication;

    public GrailsDomainClassMappingContext(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;

        final GrailsClass[] artefacts = grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE);
        for (GrailsClass grailsClass : artefacts) {
            addPersistentEntity(grailsClass.getClazz());
        }
    }

    public GrailsApplication getGrailsApplication() {
        return grailsApplication;
    }

    public MappingConfigurationStrategy getMappingSyntaxStrategy() {
        return new GrailsDomainMappingConfigurationStrategy();
    }

    @Override
    public MappingFactory getMappingFactory() {
        throw new UnsupportedOperationException("MappingFactory not supported by implementation. Defined by Grails itself.");
    }

    @Override
    protected PersistentEntity createPersistentEntity(Class javaClass) {

        GrailsDomainClass domainClass = (GrailsDomainClass) grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, javaClass.getName());
        if (domainClass == null) {
            return null;
        }
        return new GrailsDomainClassPersistentEntity(domainClass, this);
    }

    private class GrailsDomainMappingConfigurationStrategy implements MappingConfigurationStrategy {
        public boolean isPersistentEntity(Class javaClass) {
            return grailsApplication.isArtefactOfType(DomainClassArtefactHandler.TYPE, javaClass);
        }

        public List<PersistentProperty> getPersistentProperties(Class javaClass, MappingContext context) {
            return null;
        }

        /**
         * @see #getPersistentProperties(Class, org.grails.datastore.mapping.model.MappingContext, org.grails.datastore.mapping.model.ClassMapping)
         */
        public List<PersistentProperty> getPersistentProperties(PersistentEntity entity, MappingContext context, ClassMapping classMapping) {
            return null;
        }

        public List<PersistentProperty> getPersistentProperties(Class javaClass, MappingContext context, ClassMapping mapping) {
            return null;
        }

        public PersistentProperty getIdentity(Class javaClass, MappingContext context) {
            GrailsDomainClass domain = (GrailsDomainClass) grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, javaClass.getName());

            PersistentEntity persistentEntity = context.getPersistentEntity(javaClass.getName());
            GrailsDomainClassProperty identifier = domain.getIdentifier();

            return new Identity<GrailsDomainClassProperty>(persistentEntity,context, identifier.getName(),identifier.getType()) {
                public PropertyMapping getMapping() {
                    return null;
                }
            };
        }

        public IdentityMapping getDefaultIdentityMapping(ClassMapping classMapping) {
            return null;
        }

        public Set getOwningEntities(Class javaClass, MappingContext context) {
            return null;
        }

        public IdentityMapping getIdentityMapping(ClassMapping classMapping) {
            return null;
        }

        public void setCanExpandMappingContext(boolean canExpandMappingContext) {
            // noop
        }
    }
}
