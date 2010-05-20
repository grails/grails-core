/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.orm.hibernate;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.grails.commons.AbstractGrailsClass;
import org.codehaus.groovy.grails.commons.ExternalGrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.commons.GrailsDomainConfigurationUtil;
import org.codehaus.groovy.grails.exceptions.InvalidPropertyException;
import org.codehaus.groovy.grails.validation.GrailsDomainClassValidator;
import org.hibernate.EntityMode;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.AnyType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.validation.Validator;

/**
 * An implementation of the GrailsDomainClass interface that allows Classes
 * mapped in Hibernate to integrate with Grails' validation, dynamic methods
 * etc. seamlessly.
 *
 * @author Graeme Rocher
 * @since 0.1
 *        <p/>
 *        Created - 18-Feb-2006
 */
public class GrailsHibernateDomainClass extends AbstractGrailsClass implements ExternalGrailsDomainClass {

    private static final String HIBERNATE = "hibernate";

    private GrailsHibernateDomainClassProperty identifier;
    private GrailsHibernateDomainClassProperty version;

    private GrailsDomainClassProperty[] properties;

    private Map<String, GrailsHibernateDomainClassProperty> propertyMap = new LinkedHashMap<String, GrailsHibernateDomainClassProperty>();

    private Validator validator;

    @SuppressWarnings("unchecked")
    private Set subClasses = new HashSet();
    @SuppressWarnings("unchecked")
    private Map constraints = Collections.emptyMap();
    private Map<String, Object> defaultConstraints = Collections.emptyMap();

    /**
     * Contructor to be used by all child classes to create a new instance
     * and get the name right.
     *
     * @param clazz          the Grails class
     * @param sessionFactory The Hibernate SessionFactory instance
     * @param metaData       The ClassMetaData for this class retrieved from the SF
     * @param defaultConstraints The default global constraints definition
     */
    public GrailsHibernateDomainClass(Class<?> clazz, SessionFactory sessionFactory,
            ClassMetadata metaData, Map<String, Object> defaultConstraints) {
        super(clazz, "");

        new StandardAnnotationMetadata(clazz);
        String ident = metaData.getIdentifierPropertyName();
        this.defaultConstraints = defaultConstraints;
        if (ident != null) {
            Class<?> identType = getPropertyType(ident);
            identifier = new GrailsHibernateDomainClassProperty(this, ident);
            identifier.setIdentity(true);
            identifier.setType(identType);
            propertyMap.put(ident, identifier);
        }

        // configure the version property
        final int versionIndex = metaData.getVersionProperty();
        String versionPropertyName = null;
        if (versionIndex >- 1) {
            versionPropertyName = metaData.getPropertyNames()[versionIndex];
            version = new GrailsHibernateDomainClassProperty(this, versionPropertyName);
            version.setType(getPropertyType(versionPropertyName));
        }

        // configure remaining properties
        String[] propertyNames = metaData.getPropertyNames();
        for (String propertyName : propertyNames) {
            if (!propertyName.equals(ident) && !(versionPropertyName != null &&
                    propertyName.equals(versionPropertyName))) {
                GrailsHibernateDomainClassProperty prop = new GrailsHibernateDomainClassProperty(this, propertyName);
                prop.setType(getPropertyType(propertyName));
                Type hibernateType = metaData.getPropertyType(propertyName);

                // if its an association type
                if (hibernateType.isAssociationType()) {
                    prop.setAssociation(true);
                    // get the associated type from the session factory and set it on the property
                    AssociationType assType = (AssociationType) hibernateType;
                    if (assType instanceof AnyType) {
                        continue;
                    }
                    try {
                        String associatedEntity = assType.getAssociatedEntityName((SessionFactoryImplementor) sessionFactory);
                        ClassMetadata associatedMetaData = sessionFactory.getClassMetadata(associatedEntity);
                        prop.setRelatedClassType(associatedMetaData.getMappedClass(EntityMode.POJO));
                    }
                    catch (MappingException me) {
                        // other side must be a value object
                        if (hibernateType.isCollectionType()) {
                            prop.setRelatedClassType(Collection.class);
                        }
                    }
                    // configure type of relationship
                    if (hibernateType.isCollectionType()) {
                        prop.setOneToMany(true);
                    }
                    else if (hibernateType.isEntityType()) {
                        prop.setManyToOne(true);
                        // might not really be true, but for our purposes this is ok
                        prop.setOneToOne(true);
                    }
                }
                propertyMap.put(propertyName, prop);
            }
        }

        properties = propertyMap.values().toArray(new GrailsDomainClassProperty[propertyMap.size()]);
        // process the constraints
        evaluateConstraints();
    }

    /**
     * Evaluates the constraints closure to build the list of constraints
     * @param defaultContraints The default global constraints definition
     */
    @SuppressWarnings("unchecked")
    private void evaluateConstraints() {
        Map existing = (Map) getPropertyOrStaticPropertyOrFieldValue(GrailsDomainClassProperty.CONSTRAINTS, Map.class);
        if (existing == null) {
            constraints = GrailsDomainConfigurationUtil.evaluateConstraints(
                    getClazz(), getProperties(), defaultConstraints);
        }
        else {
            constraints = existing;
        }
    }

    @SuppressWarnings("unchecked")
    public boolean isOwningClass(Class domainClass) {
        return false;
    }

    public GrailsDomainClassProperty[] getProperties() {
        return properties;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public GrailsDomainClassProperty[] getPersistantProperties() {
        return properties;
    }

    public GrailsDomainClassProperty[] getPersistentProperties() {
        return properties;
    }

    public GrailsDomainClassProperty getIdentifier() {
        return identifier;
    }

    public GrailsDomainClassProperty getVersion() {
        return version;
    }

    public GrailsDomainClassProperty getPropertyByName(String name) {
        if (propertyMap.containsKey(name)) {
            return propertyMap.get(name);
        }

        throw new InvalidPropertyException("No property found for name ["+name+"] for class ["+getClazz()+"]");
    }

    public String getFieldName(String propertyName) {
        return getPropertyByName(propertyName).getFieldName();
    }

    public boolean hasSubClasses() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public Map getMappedBy() {
        return Collections.emptyMap();
    }

    public boolean hasPersistentProperty(String propertyName) {
        for (GrailsDomainClassProperty persistantProperty : properties) {
            if (persistantProperty.getName().equals(propertyName)) return true;
        }
        return false;
    }

    public void setMappingStrategy(String strategy) {
        // do nothing, read-only
    }

    public boolean isOneToMany(String propertyName) {
        GrailsDomainClassProperty prop = getPropertyByName(propertyName);
        return prop != null && prop.isOneToMany();
    }

    public boolean isManyToOne(String propertyName) {
        GrailsDomainClassProperty prop = getPropertyByName(propertyName);
        return prop != null && prop.isManyToOne();
    }

    public boolean isBidirectional(String propertyName) {
        return false;
    }

    public Class<?> getRelatedClassType(String propertyName) {
        GrailsDomainClassProperty prop = getPropertyByName(propertyName);
        if (prop == null) {
            return null;
        }

        return prop.getReferencedPropertyType();
    }

    @SuppressWarnings("unchecked")
    public Map getConstrainedProperties() {
        return constraints;
    }

    public Validator getValidator() {
        if (validator == null) {
            GrailsDomainClassValidator gdcv = new GrailsDomainClassValidator();
            gdcv.setDomainClass(this);
            validator = gdcv;
        }
        return validator;
    }

    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    public String getMappingStrategy() {
        return HIBERNATE;
    }

    @SuppressWarnings("unchecked")
    public Set getSubClasses() {
        return subClasses;
    }

    public void refreshConstraints() {
        evaluateConstraints();
    }

    public boolean isRoot() {
        return getClazz().getSuperclass().equals(Object.class);
    }

    @SuppressWarnings("unchecked")
    public Map getAssociationMap() {
        return Collections.emptyMap();
    }
}
