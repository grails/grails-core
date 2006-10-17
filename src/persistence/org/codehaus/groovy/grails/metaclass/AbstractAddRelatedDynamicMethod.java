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
package org.codehaus.groovy.grails.metaclass;

import java.util.Collection;
import java.util.regex.Pattern;

import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethodInvocation;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

/**
 * Abstract class for relationship management methods
 *
 * @author Graeme Rocher
 *         <p/>
 *         Date: Sep 19, 2006
 *         Time: 8:18:56 AM
 * @since 0.3
 */
public abstract class AbstractAddRelatedDynamicMethod extends
		AbstractDynamicMethodInvocation {

	public AbstractAddRelatedDynamicMethod(Pattern p) {
		super(p);
	}

	/**
	 * @param target
	 * @param toAdd
	 */
	protected void addObjectToTarget(Object target, Object toAdd, String collectionName) {
		BeanWrapper bean = new BeanWrapperImpl(target);
	    Collection elements = (Collection)bean.getPropertyValue(collectionName);
	    if(elements == null) {
	        Class colType = bean.getPropertyType(collectionName);
	
	        elements = GrailsClassUtils.createConcreteCollection(colType);
	    }
	
	    
	    handeBidirectional(target, toAdd, getDomainClassProperty(collectionName));
	    elements.add(toAdd);
	    bean.setPropertyValue(collectionName,elements);
	}

	/**
	 * Implements need to provide the domain class property for the association
	 * @param collectionName The name of the collection
	 * @return The property for the collection
	 */
	protected abstract GrailsDomainClassProperty getDomainClassProperty(String collectionName);

	/**
	 * @param target
	 * @param toAdd
	 * @param grailsProperty 
	 */
	private void handeBidirectional(Object target, Object toAdd, GrailsDomainClassProperty grailsProperty) {
		if(grailsProperty.isBidirectional()) {
	        GrailsDomainClassProperty otherSide = grailsProperty.getOtherSide();
	        if(otherSide != null) {
	            BeanWrapper other = new BeanWrapperImpl(toAdd);
	            other.setPropertyValue(otherSide.getName(),target);
	        }
	    }
	}

}