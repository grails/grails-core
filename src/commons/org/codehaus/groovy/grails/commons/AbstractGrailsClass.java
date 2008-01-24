/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.commons;

import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.exceptions.NewInstanceCreationException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Abstract base class for Grails types that provides common functionality for
 * evaluating conventions within classes
 * 
 * @author Steven Devijver
 * @author Graeme Rocher
 * 
 * @since 0.1
 * 
 *  Created: Jul 2, 2005
 */
public abstract class AbstractGrailsClass implements GrailsClass {



	private Class clazz = null;
    private String fullName = null;
    private String name = null;
    private String packageName = null;
    private BeanWrapper reference = null;
    private String naturalName;
    private String shortName;
    private MetaClass metaClass;



    /**
     * <p>Contructor to be used by all child classes to create a
     * new instance and get the name right.
     *
     * @param clazz the Grails class
     * @param trailingName the trailing part of the name for this class type
     */
    public AbstractGrailsClass(Class clazz, String trailingName) {
        super();
        setClazz(clazz);

        this.reference = new BeanWrapperImpl(newInstance());
        this.fullName = clazz.getName();
        this.packageName = ClassUtils.getPackageName(clazz);
        this.naturalName = GrailsClassUtils.getNaturalName(clazz.getName());
        this.shortName = getShortClassname(clazz);        
        this.name = GrailsClassUtils.getLogicalName(clazz, trailingName);
    }

	public String getShortName() {
        return this.shortName;
    }

    private void setClazz(Class clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Clazz parameter should not be null");
        }
        this.clazz = clazz;
    }

    public Class getClazz() {
        return this.clazz;
    }

    public Object newInstance() {
        try {
            Constructor defaultConstructor = getClazz().getDeclaredConstructor(new Class[]{});
            if(!defaultConstructor.isAccessible()) defaultConstructor.setAccessible(true);
            return defaultConstructor.newInstance(new Object[]{});
        } catch (Exception e) {
            Throwable targetException = null;
            if (e instanceof InvocationTargetException) {
                targetException = ((InvocationTargetException)e).getTargetException();
            } else {
                targetException = e;
            }
            throw new NewInstanceCreationException("Could not create a new instance of class [" + getClazz().getName() + "]!", targetException);
        }
    }

    public String getName() {
        return this.name;
    }

    public String getNaturalName() {
        return this.naturalName;
    }

    public String getFullName() {
        return this.fullName;
    }

    public String getPropertyName() {
        return GrailsClassUtils.getPropertyNameRepresentation(getShortName());
    }

    public String getLogicalPropertyName() {

        final String logicalName = getName();
        if(StringUtils.isBlank(logicalName)) {
            return GrailsClassUtils.getPropertyNameRepresentation(getShortName());            
        }
        return GrailsClassUtils.getPropertyNameRepresentation(logicalName);
    }

    public String getPackageName() {
        return this.packageName;
    }

    private static String getShortClassname(Class clazz) {
        return ClassUtils.getShortClassName(clazz);
    }

    /**
     * <p>The reference instance is used to get configured property values.
     *
     * @return BeanWrapper instance that holds reference
     */
    public BeanWrapper getReference() {
        Object obj = this.reference.getWrappedInstance();
        if(obj instanceof GroovyObject) {
            ((GroovyObject)obj).setMetaClass(GroovySystem.getMetaClassRegistry().getMetaClass(getClazz()));
        }
        return this.reference;
    }

    /**
     * <p>Looks for a property of the reference instance with a given name and type.</p>
     * <p>If found its value is returned. We follow the Java bean conventions with augmentation for groovy support
     * and static fields/properties. We will therefore match, in this order:
     * </p>
     * <ol>
     * <li>Standard public bean property (with getter or just public field, using normal introspection)
     * <li>Public static property with getter method
     * <li>Public static field
     * </ol>
     *
     *
     * @return property value or null if no property or static field was found
     */
    protected Object getPropertyOrStaticPropertyOrFieldValue(String name, Class type) {
        BeanWrapper ref = getReference();
        Object value = null;
        if (ref.isReadableProperty(name)) {
            value = ref.getPropertyValue(name);
        }
        else if (GrailsClassUtils.isPublicField(ref.getWrappedInstance(), name))
        {
            value = GrailsClassUtils.getFieldValue(ref.getWrappedInstance(), name);
        }
        else
        {
            value = GrailsClassUtils.getStaticPropertyValue(clazz, name);
        }
        if ((value != null) && GrailsClassUtils.isGroovyAssignableFrom( type, value.getClass())) {
            return value;
        }
        return null;
    }

    /**
     * Get the value of the named property, with support for static properties in both Java and Groovy classes
     * (which as of Groovy JSR 1.0 RC 01 only have getters in the metaClass)
     * @param name
     * @param type
     * @return The property value or null
     */
    public Object getPropertyValue(String name, Class type) {

        // Handle standard java beans normal or static properties
        BeanWrapper ref = getReference();
        Object value = null;
        if (ref.isReadableProperty(name)) {
            value = ref.getPropertyValue(name);
        }
        else{
            // Groovy workaround
            Object inst = ref.getWrappedInstance();
            if (inst instanceof GroovyObject)
            {
            	final Map properties = DefaultGroovyMethods.getProperties(inst);
            	if(properties.containsKey(name)) {
            		value = properties.get(name);
            	}
            }
        }

        if(value != null && (type.isAssignableFrom(value.getClass())
            || GrailsClassUtils.isMatchBetweenPrimativeAndWrapperTypes(type, value.getClass()))) {
            return value;
        }
        else
        {
            return null;
        }
    }

    /* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.commons.GrailsClass#getPropertyValue(java.lang.String)
	 */
	public Object getPropertyValue(String name) {
		return getPropertyOrStaticPropertyOrFieldValue(name, Object.class);
	}

    

    /* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.commons.GrailsClass#hasProperty(java.lang.String)
	 */
	public boolean hasProperty(String name) {
		return getReference().isReadableProperty(name);
	}

	/**
	 * @return the metaClass
	 */
	public MetaClass getMetaClass() {
		return GrailsClassUtils.getExpandoMetaClass(clazz);
	}

    public String toString() {
        return "Artefact > " + getName();
    }
}
