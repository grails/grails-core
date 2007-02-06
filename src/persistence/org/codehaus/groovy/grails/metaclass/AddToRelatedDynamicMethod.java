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

import groovy.lang.MissingMethodException;

import java.util.Map;
import java.util.regex.Pattern;

import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;

/**
 * Implements the relationship management method for explicitly adding elements to a named
 * relationship like author.add(to:'books', book)
 *
 * @author Graeme Rocher
 *         <p/>
 *         Date: Sep 19, 2006
 *         Time: 8:18:56 AM
 *         
 * @since 0.3        
 */

public class AddToRelatedDynamicMethod extends AbstractAddRelatedDynamicMethod {

	public static final Pattern METHOD_PATTERN = Pattern.compile("^add$");
	public static final String METHOD_NAME = "add";
	public static final String TO_ARGUMENT = "to";
	private GrailsDomainClass domainClass;
	

	public AddToRelatedDynamicMethod(GrailsDomainClass domainClass) {
		super(AddToRelatedDynamicMethod.METHOD_PATTERN);
		this.domainClass = domainClass;
	}

	public Object invoke(Object target, String methodName, Object[] arguments) {
		if(arguments.length < 2)
			throw new MissingMethodException(METHOD_NAME,target.getClass(),arguments);
        if(arguments[1] == null) {
        	throw new IllegalArgumentException("Argument to method [add] cannot be null");
        }
         
		
		String collectionName;
		if(arguments[0] instanceof Map) {
			collectionName = (String)((Map)arguments[0]).get(TO_ARGUMENT);
			GrailsDomainClassProperty property = getDomainClassProperty(collectionName);
			
	        if(!arguments[1].getClass().equals(property.getReferencedPropertyType())) {
	            throw new MissingMethodException(METHOD_NAME,target.getClass(),arguments);
	        }
			
		}
		else {
			throw new MissingMethodException(METHOD_NAME,target.getClass(),arguments);
		}
		Object toAdd = arguments[1];
		addObjectToTarget(target, toAdd, collectionName);
		return target;
	}

	protected GrailsDomainClassProperty getDomainClassProperty(String collectionName) {
		return this.domainClass.getPropertyByName(collectionName);
	}

}
