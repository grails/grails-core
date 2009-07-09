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

import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import groovy.lang.MetaClassRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.AbstractGrailsClass;
import org.codehaus.groovy.grails.commons.ExternalGrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethodsInterceptor;
import org.codehaus.groovy.grails.commons.metaclass.DynamicMethods;
import org.codehaus.groovy.grails.commons.metaclass.Interceptor;
import org.codehaus.groovy.grails.commons.metaclass.ProxyMetaClass;
import org.codehaus.groovy.grails.validation.GrailsDomainClassValidator;
import org.codehaus.groovy.grails.validation.metaclass.ConstraintsEvaluatingDynamicProperty;
import org.hibernate.EntityMode;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;
import org.springframework.beans.BeanWrapper;
import org.springframework.validation.Validator;

import java.beans.IntrospectionException;
import java.util.*;

/**
 * An implementation of the GrailsDomainClass interface that allows Classes
 * mapped in Hibernate to integrate with Grails' validation, dynamic methods
 * etc. seamlessly
 * 
 * @author Graeme Rocher
 * @since 0.1
 * 
 * Created - 18-Feb-2006
 */
public class GrailsHibernateDomainClass extends AbstractGrailsClass implements ExternalGrailsDomainClass {

    private static final Log LOG = LogFactory.getLog(GrailsHibernateDomainClass.class);

    private static final String HIBERNATE = "hibernate";

    private GrailsHibernateDomainClassProperty identifier;

    private GrailsDomainClassProperty[] properties;

    private Map propertyMap = new LinkedHashMap();

    private Validator validator;

    private Set subClasses = new HashSet();

    private Map constraints = Collections.EMPTY_MAP;

    /**
         * <p>
         * Contructor to be used by all child classes to create a new instance
         * and get the name right.
         * 
         * @param clazz
         *                the Grails class
         * @param sessionFactory
         *                The Hibernate SessionFactory instance
         * @param metaData
         *                The ClassMetaData for this class retrieved from the SF
         */
    public GrailsHibernateDomainClass(Class clazz, SessionFactory sessionFactory, ClassMetadata metaData) {
	super(clazz, "");

	BeanWrapper bean = getReference();
	// configure identity property
	String ident = metaData.getIdentifierPropertyName();

	if (ident != null) {
	    Class identType = bean.getPropertyType(ident);
	    this.identifier = new GrailsHibernateDomainClassProperty(this, ident);
	    this.identifier.setIdentity(true);
	    this.identifier.setType(identType);
        propertyMap.put(ident, identifier);
    }


	// configure remaining properties
	String[] propertyNames = metaData.getPropertyNames();
	for (int i = 0; i < propertyNames.length; i++) {
	    String propertyName = propertyNames[i];
	    if (!propertyName.equals(ident)) {
		GrailsHibernateDomainClassProperty prop = new GrailsHibernateDomainClassProperty(this, propertyName);
		prop.setType(bean.getPropertyType(propertyName));
		Type hibernateType = metaData.getPropertyType(propertyName);
		// if its an association type
		if (hibernateType.isAssociationType()) {
		    prop.setAssociation(true);
		    // get the associated type from the session factory
		    // and set it on the property
		    AssociationType assType = (AssociationType) hibernateType;
		    if (assType instanceof org.hibernate.type.AnyType)
			continue;
		    try {
			String associatedEntity = assType.getAssociatedEntityName((SessionFactoryImplementor) sessionFactory);
			ClassMetadata associatedMetaData = sessionFactory.getClassMetadata(associatedEntity);
			prop.setRelatedClassType(associatedMetaData.getMappedClass(EntityMode.POJO));
		    } catch (MappingException me) {
			// other side must be a value object
			if (hibernateType.isCollectionType()) {
			    prop.setRelatedClassType(Collection.class);
			}
		    }
		    // configure type of relationship
		    if (hibernateType.isCollectionType()) {
			prop.setOneToMany(true);
		    } else if (hibernateType.isEntityType()) {
			prop.setManyToOne(true);
			// might not really be true, but for our
			// purposes this is ok
			prop.setOneToOne(true);
		    }
		}
		propertyMap.put(propertyName, prop);
	    }
	}

	this.properties = (GrailsDomainClassProperty[]) propertyMap.values().toArray(new GrailsDomainClassProperty[propertyMap.size()]);
	// process the constraints
	evaluateConstraints();
    }

    /**
         * Evaluates the constraints closure to build the list of constraints
         * 
         */
    private void evaluateConstraints() {
	Map existing = (Map) getPropertyOrStaticPropertyOrFieldValue(GrailsDomainClassProperty.CONSTRAINTS, Map.class);
	if (existing == null) {
	    Object instance = getReference().getWrappedInstance();
	    try {
		DynamicMethods interceptor = new AbstractDynamicMethodsInterceptor() {
		};

		interceptor.addDynamicProperty(new ConstraintsEvaluatingDynamicProperty());
		MetaClassRegistry metaRegistry = GroovySystem.getMetaClassRegistry();
		MetaClass meta = metaRegistry.getMetaClass(instance.getClass());

		try {
		    ProxyMetaClass pmc = new ProxyMetaClass(metaRegistry, instance.getClass(), meta);
		    pmc.setInterceptor((Interceptor) interceptor);

		    this.constraints = (Map) pmc.getProperty(getClazz(), instance, GrailsDomainClassProperty.CONSTRAINTS, false, false);
		} finally {
		    metaRegistry.setMetaClass(instance.getClass(), meta);
		}

	    } catch (IntrospectionException e) {
		LOG.error("Introspection error reading domain class [" + getFullName() + "] constraints: " + e.getMessage(), e);
	    }
	} else {
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
	return (GrailsDomainClassProperty) propertyMap.get(name);
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
        for (int i = 0; i < properties.length; i++) {
            GrailsDomainClassProperty persistantProperty = properties[i];
            if(persistantProperty.getName().equals(propertyName)) return true;
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
	this.evaluateConstraints();
    }

    public boolean isRoot() {
	return getClazz().getSuperclass().equals(Object.class);
    }

    public Map getAssociationMap() {
	return Collections.EMPTY_MAP;
    }
}
