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

import groovy.lang.MetaClass;
import org.springframework.beans.BeanWrapper;

/**
 * <p>This interface represents any class in a Grails application.
 * 
 * @author Steven Devijver
 * @author Graeme Rocher
 * @since 0.1
 * 
 * Created: Jul 2, 2005
 */
public interface GrailsClass {
	
	/**
	 * <p>Gets the initial value of the given property on the class</p>
	 * @param name The name of the property
	 * @return The initial value
	 */
	public Object getPropertyValue(String name);
	
	/**
	 * <p>Returns true if the class has the specified property</p>
	 * @param name The name of the property
	 * @return True if it does
	 */
	public boolean hasProperty(String name);

    /**
     * <p>Creates a new instance of this class.</p>
     *
     * <p>This method can be used as factory method in the Spring application context.</p>
     * @return a new instance of this class
     */
    public Object newInstance();

    /**
     * <p>Returns the logical name of the class in the application without the trailing convention part if applicable
     * and without the package name.</p>
     *
     * @return the logical name
     */
    public String getName();


    /**
     * <p>Returns the short name of the class without package prefix</p>
     *
     * @return The short name
     */
    public String getShortName();
    /**
     * <p>Returns the full name of the class in the application with the the trailing convention part and with
     * the package name.</p>
     *
     * @return the full name
     */
    public String getFullName();

    /**
     * <p>Returns the name of the class as a property name</p>
     *
     * @return The property name representation
     */
    public String getPropertyName();


    /**
     * <p>Returns the logical name of the class as a property name</p>
     *
     * @return The logical property name
     */
    public String getLogicalPropertyName();

    /**
     * <p>Returns the name of the property in natural terms (eg. 'lastName' becomes 'Last Name')
     * @return The natural property name</p>
     */
    public String getNaturalName();

    /**
     * <p>Returns the package name of the class.</p>
     *
     * @return the package name
     */
    public String getPackageName();

    /**
     * <p> Returns the actual clazz represented by the GrailsClass</p>
     *
     * @return the class
     */
    public Class getClazz();
    
    /**
     * 
     * @return The MetaClass for this Grails class
     */
    public MetaClass getMetaClass();

    BeanWrapper getReference();

    /**
     * Obtains a property value for the given name and type
     * @param name The name
     * @param type The type
     *
     * @return  The property value
     */
    Object getPropertyValue(String name, Class type);
}
