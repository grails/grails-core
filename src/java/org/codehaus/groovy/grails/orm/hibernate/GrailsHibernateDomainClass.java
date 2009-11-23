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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.AbstractGrailsClass;
import org.codehaus.groovy.grails.commons.ExternalGrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.commons.GrailsDomainConfigurationUtil;
import org.codehaus.groovy.grails.validation.GrailsDomainClassValidator;
import org.hibernate.EntityMode;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.AnyType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;
import org.springframework.validation.Validator;

import java.util.*;

/**
 * An implementation of the GrailsDomainClass interface that allows Classes
 * mapped in Hibernate to integrate with Grails' validation, dynamic methods
 * etc. seamlessly
 *
 * @author Graeme Rocher
 * @since 0.1
 *        <p/>
 *        Created - 18-Feb-2006
 */
public class GrailsHibernateDomainClass extends AbstractGrailsClass implements ExternalGrailsDomainClass {

    private static final Log LOG = LogFactory.getLog(GrailsHibernateDomainClass.class);

    private static final String HIBERNATE = "hibernate";

    private GrailsHibernateDomainClassProperty identifier;

    private GrailsDomainClassProperty[] properties;

    private Map<String, GrailsHibernateDomainClassProperty> propertyMap = new LinkedHashMap<String, GrailsHibernateDomainClassProperty>();

    private Validator validator;

    private Set subClasses = new HashSet();

    private Map constraints = Collections.EMPTY_MAP;
    private Map defaultConstraints = Collections.EMPTY_MAP;

    /**
     * <p/>
     * Contructor to be used by all child classes to create a new instance
     * and get the name right.
     *
     * @param clazz          the Grails class
     * @param sessionFactory The Hibernate SessionFactory instance
     * @param metaData       The ClassMetaData for this class retrieved from the SF
     * @param defaultContraints The default global constraints definition
     */
    public GrailsHibernateDomainClass(Class clazz, SessionFactory sessionFactory, ClassMetadata metaData, Map<String, Object> defaultContraints) {
        super(clazz, "");

        // configure identity property
        String ident = metaData.getIdentifierPropertyName();
        this.defaultConstraints = defaultContraints;

        if (ident != null) {
            Class identType = getPropertyType(ident);
            this.identifier = new GrailsHibernateDomainClassProperty(this, ident);
            this.identifier.setIdentity(true);
            this.identifier.setType(identType);
            propertyMap.put(ident, identifier);
        }


        // configure remaining properties
        String[] propertyNames = metaData.getPropertyNames();
        for (String propertyName : propertyNames) {
            if (!propertyName.equals(ident)) {
                GrailsHibernateDomainClassProperty prop = new GrailsHibernateDomainClassProperty(this, propertyName);
                prop.setType(getPropertyType(propertyName));
                Type hibernateType = metaData.getPropertyType(propertyName);
                // if its an association type
                if (hibernateType.isAssociationType()) {
                    prop.setAssociation(true);
                    // get the associated type from the session factory
                    // and set it on the property
                    AssociationType assType = (AssociationType) hibernateType;
                    if (assType instanceof AnyType)
                        continue;
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
                        // might not really be true, but for our
                        // purposes this is ok
                        prop.setOneToOne(true);
                    }
                }
                propertyMap.put(propertyName, prop);
            }
        }

        this.properties = propertyMap.values().toArray(new GrailsDomainClassProperty[propertyMap.size()]);
        // process the constraints
        evaluateConstraints(defaultContraints);
    }

    /**
     * Evaluates the constraints closure to build the list of constraints
     * @param defaultContraints The default global constraints definition
     */
    private void evaluateConstraints(Map<String, Object> defaultContraints) {
        Map existing = (Map) getPropertyOrStaticPropertyOrFieldValue(GrailsDomainClassProperty.CONSTRAINTS, Map.class);
        if (existing == null) {
            this.constraints = GrailsDomainConfigurationUtil.evaluateConstraints(getReferenceInstance(), getProperties(), defaultContraints);
        }
        else {
            this.constraints = existing;
        }
    }

    public boolean isOwningClass(Class domainClass) {
        return false;
    }

    public GrailsDomainClassProperty[] getProperties() {
        return this.properties;
    }

    /**
     * @deprecated
     */
    public GrailsDomainClassProperty[] getPersistantProperties() {
        return this.properties;
    }

    public GrailsDomainClassProperty[] getPersistentProperties() {
        return this.properties;
    }

    public GrailsDomainClassProperty getIdentifier() {
        return this.identifier;
    }

    public GrailsDomainClassProperty getVersion() {
        throw new UnsupportedOperationException("Method 'getVersion' is not supported by implementation");
    }

    public GrailsDomainClassProperty getPropertyByName(String name) {
        return propertyMap.get(name);
    }

    public String getFieldName(String propertyName) {
        throw new UnsupportedOperationException("Method 'getFieldName' is not supported by implementation");
    }

    public boolean hasSubClasses() {
        return false;
    }

    public Map getMappedBy() {
        return Collections.EMPTY_MAP;
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
        throw new UnsupportedOperationException("Method 'isBidirectional' is not supported by implementation");
    }

    public Class getRelatedClassType(String propertyName) {
        GrailsDomainClassProperty prop = getPropertyByName(propertyName);
        if (prop == null)
            return null;
        else {
            return prop.getReferencedPropertyType();
        }
    }

    public Map getConstrainedProperties() {
        return this.constraints;
    }

    public Validator getValidator() {
        if (this.validator == null) {
            org.codehaus.groovy.grails.validation.GrailsDomainClassValidator gdcv = new GrailsDomainClassValidator();
            gdcv.setDomainClass(this);
            this.validator = gdcv;
        }
        return this.validator;
    }

    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    public String getMappingStrategy() {
        return HIBERNATE;
    }

    public Set getSubClasses() {
        return this.subClasses;
    }

    public void refreshConstraints() {
        this.evaluateConstraints(this.defaultConstraints);
    }

    public boolean isRoot() {
        return getClazz().getSuperclass().equals(Object.class);
    }

    public Map getAssociationMap() {
        return Collections.EMPTY_MAP;
    }
}
