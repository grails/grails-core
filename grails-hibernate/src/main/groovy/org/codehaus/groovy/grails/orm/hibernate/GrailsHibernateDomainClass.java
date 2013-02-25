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

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.grails.commons.AbstractGrailsClass;
import org.codehaus.groovy.grails.commons.ExternalGrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.exceptions.InvalidPropertyException;
import org.codehaus.groovy.grails.validation.ConstraintsEvaluator;
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator;
import org.codehaus.groovy.grails.validation.GrailsDomainClassValidator;
import org.hibernate.EntityMode;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.AnyType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.validation.Validator;

/**
 * An implementation of the GrailsDomainClass interface that allows Classes
 * mapped in Hibernate to integrate with Grails' validation, dynamic methods
 * etc. seamlessly.
 *
 * @author Graeme Rocher
 * @since 0.1
 */
@SuppressWarnings("rawtypes")
public class GrailsHibernateDomainClass extends AbstractGrailsClass implements ExternalGrailsDomainClass {

    public static final String HIBERNATE = "hibernate";

    private GrailsHibernateDomainClassProperty identifier;
    private GrailsHibernateDomainClassProperty version;

    private GrailsDomainClassProperty[] properties;

    private Map<String, GrailsHibernateDomainClassProperty> propertyMap = new LinkedHashMap<String, GrailsHibernateDomainClassProperty>();

    private Validator validator;
    private GrailsApplication application;
    private SessionFactory sessionFactory;
    private String sessionFactoryName;

    private Set subClasses = new HashSet();
    private Map constraints = Collections.emptyMap();

    /**
     * Contructor to be used by all child classes to create a new instance
     * and get the name right.
     *
     * @param clazz          the Grails class
     * @param sessionFactory The Hibernate SessionFactory instance
     * @param sessionFactoryName
     * @param application
     * @param metaData       The ClassMetaData for this class retrieved from the SF
     */
    public GrailsHibernateDomainClass(Class<?> clazz, SessionFactory sessionFactory, String sessionFactoryName,
            GrailsApplication application, ClassMetadata metaData) {
        super(clazz, "");
        this.application = application;
        this.sessionFactory = sessionFactory;
        this.sessionFactoryName = sessionFactoryName;

        new StandardAnnotationMetadata(clazz);
        String ident = metaData.getIdentifierPropertyName();
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

                PropertyDescriptor pd = GrailsClassUtils.getProperty(clazz,propertyName);
                if (pd == null) continue;

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

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public String getSessionFactoryName() {
        return sessionFactoryName;
    }

    /**
     * Evaluates the constraints closure to build the list of constraints.
     */
    private void evaluateConstraints() {
        Map existing = (Map) getPropertyOrStaticPropertyOrFieldValue(GrailsDomainClassProperty.CONSTRAINTS, Map.class);
        if (existing == null) {
            constraints = getConstraintsEvaluator().evaluate(getClazz(), getProperties());
        }
        else {
            constraints = existing;
        }
    }

    private ConstraintsEvaluator getConstraintsEvaluator() {
        if (application != null && application.getMainContext() != null) {
            final ApplicationContext context = application.getMainContext();
            if (context.containsBean(ConstraintsEvaluator.BEAN_NAME)) {
                return context.getBean(ConstraintsEvaluator.BEAN_NAME, ConstraintsEvaluator.class);
            }
        }
        return new DefaultConstraintEvaluator();
    }

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

    public GrailsDomainClassProperty getPersistentProperty(String name) {
        if (propertyMap.containsKey(name)) {
            return propertyMap.get(name);
        }

        return null;
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

    public Map getConstrainedProperties() {
        return constraints;
    }

    public Validator getValidator() {
        if (validator == null) {
            GrailsDomainClassValidator gdcv = new GrailsDomainClassValidator();
            gdcv.setDomainClass(this);
            MessageSource messageSource = application.getMainContext().getBean(MessageSource.class);
            gdcv.setMessageSource(messageSource);
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

    public Map getAssociationMap() {
        return Collections.emptyMap();
    }
}
