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

import java.util.regex.Pattern;

import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;

/**
 * Implements the relationships management method add*. For example an Author with many Books
 * would have addBook etc.
 *
 * @author Graeme Rocher
 *         <p/>
 *         Date: Sep 19, 2006
 *         Time: 8:18:56 AM
 *  @since 0.3
 *         
 */
public class AddRelatedDynamicMethod extends AbstractAddRelatedDynamicMethod {

    private String methodName;
	private GrailsDomainClassProperty property;

    /**
     * Creates a method to manage relationships on a domain class like Author.addBook(book)
     *
     * @param property The domain class property to manage
     */
    public AddRelatedDynamicMethod(GrailsDomainClassProperty property) {

        super(Pattern.compile("^add" + GrailsClassUtils.getShortName(property.getReferencedPropertyType()) + '$'));
        this.methodName = "add" + GrailsClassUtils.getShortName(property.getReferencedPropertyType());
        this.property = property;
    }

    public Object invoke(Object target, String methodName, Object[] arguments) {
        if(arguments.length == 0 ) {
           throw new MissingMethodException(this.methodName,target.getClass(),arguments);
        }
        if(arguments[0] == null) {
        	throw new IllegalArgumentException("Argument to ["+ this.methodName +"] cannot be null");
        }
        if(!arguments[0].getClass().equals(property.getReferencedPropertyType())) {
           throw new MissingMethodException(this.methodName,target.getClass(),arguments);
        }

        Object toAdd = arguments[0];
        
        addObjectToTarget(target, toAdd, property.getName());

        return target;
    }

	protected GrailsDomainClassProperty getDomainClassProperty(String collectionName) {
		return this.property;
	}
}
