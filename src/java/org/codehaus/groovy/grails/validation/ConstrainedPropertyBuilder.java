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
package org.codehaus.groovy.grails.validation;

import grails.util.GrailsUtil;
import groovy.lang.MissingMethodException;
import groovy.util.BuilderSupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.grails.commons.ClassPropertyFetcher;
import org.springframework.beans.InvalidPropertyException;

/**
 * Builder used as a delegate within the "constraints" closure of GrailsDomainClass instances 
 * 
 * @author Graeme Rocher
 * @since 10-Nov-2005
 */
public class ConstrainedPropertyBuilder extends BuilderSupport {

	private Map<String, ConstrainedProperty> constrainedProperties = new HashMap<String, ConstrainedProperty>();
    private List<String> sharedConstraints = new ArrayList<String>();
	private int order = 1;
	private Class targetClass;
	private ClassPropertyFetcher classPropertyFetcher;
    private static final String SHARED_CONSTRAINT = "shared";

    public ConstrainedPropertyBuilder(Object target) {		
		this(target.getClass());
	}
    
    public ConstrainedPropertyBuilder(Class targetClass) {
		this.targetClass = targetClass;
		this.classPropertyFetcher = ClassPropertyFetcher.forClass(targetClass);
	}


    public List<String> getSharedConstraints() {
        return Collections.unmodifiableList(sharedConstraints);
    }

    protected Object createNode(Object name, Map attributes) {
		// we do this so that missing property exception is throw if it doesn't exist

        try {
			String property = (String)name;
			ConstrainedProperty cp;
			if(constrainedProperties.containsKey(property)) {
				cp = constrainedProperties.get(property);
			}
			else {				
				cp = new ConstrainedProperty(this.targetClass, property, classPropertyFetcher.getPropertyType(property));
				cp.setOrder(order++);
				constrainedProperties.put( property, cp );
			}
            for (Object o : attributes.keySet()) {
                String constraintName = (String) o;
                final Object value = attributes.get(constraintName);
                if(SHARED_CONSTRAINT.equals(constraintName)) {
                    if(value != null)
                        sharedConstraints.add(value.toString());
                    continue;
                }
                if (cp.supportsContraint(constraintName)) {
                    cp.applyConstraint(constraintName, value);
                }
                else {
                    if (ConstrainedProperty.hasRegisteredConstraint(constraintName)) {
                        // constraint is registered but doesn't support this property's type
                        GrailsUtil.warn("Property [" + cp.getPropertyName() + "] of domain class " + targetClass.getName() + " has type [" + cp.getPropertyType().getName() + "] and doesn't support constraint [" + constraintName + "]. This constraint will not be checked during validation.");
                    }
                    else {
                        // in the case where the constraint is not supported we still retain meta data
                        // about the constraint in case its needed for other things
                        cp.addMetaConstraint(constraintName, value);
                    }
                }
            }
			return cp;
		}
		catch(InvalidPropertyException ipe) {
			throw new MissingMethodException((String)name,targetClass,new Object[]{ attributes});
		}		
	}

	protected Object createNode(Object name, Map attributes, Object value) {
		throw new MissingMethodException((String)name,targetClass,new Object[]{ attributes,value});
	}

	protected void setParent(Object parent, Object child) {
		// do nothing
	}	
	protected Object createNode(Object name) {
		return createNode(name, Collections.EMPTY_MAP);
	}
	
	protected Object createNode(Object name, Object value) {
		return createNode(name,Collections.EMPTY_MAP,value);
	}	
	
	public Map<String, ConstrainedProperty> getConstrainedProperties() {
		return this.constrainedProperties;
	}

}
