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

import org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethodInvocation;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.regex.Pattern;
import java.util.*;

import groovy.lang.MissingMethodException;

/**
 * To change this template use File | Settings | File Templates.
 *
 * @author Graeme Rocher
 *         <p/>
 *         Date: Sep 19, 2006
 *         Time: 8:18:56 AM
 */
public class AddRelatedDynamicMethod extends AbstractDynamicMethodInvocation {

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

    public Object invoke(Object target, Object[] arguments) {
        if(arguments.length == 0) {
           throw new MissingMethodException(methodName,target.getClass(),arguments);
        }
        if(!arguments[0].getClass().equals(property.getReferencedPropertyType())) {
           throw new MissingMethodException(methodName,target.getClass(),arguments);
        }

        BeanWrapper bean = new BeanWrapperImpl(target);

        Collection elements = (Collection)bean.getPropertyValue(property.getName());
        if(elements == null) {
            Class colType = bean.getPropertyType(property.getName());

            if(colType.equals(List.class)) {
                elements = new ArrayList();
            }
            else if(colType.equals(SortedSet.class)) {
                elements = new TreeSet();
            }
            else {
                elements = new HashSet();
            }
        }

        Object toAdd = arguments[0];
        if(property.isBidirectional()) {
            GrailsDomainClassProperty otherSide = property.getOtherSide();
            if(otherSide != null) {
                BeanWrapper other = new BeanWrapperImpl(toAdd);
                other.setPropertyValue(otherSide.getName(),target);
            }
        }
        elements.add(toAdd);
        bean.setPropertyValue(property.getName(),elements);


        return target;
    }
}
