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
package grails.util;

import groovy.util.Proxy;

import java.util.Map;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * Extends the Groovy Proxy implementation and adds proxying of property getters/setters
 * 
* @author Graeme Rocher
* @author Jonathan Carlson
 * 
* @since Oct 20, 2005
*/
public class ExtendProxy extends Proxy {

    public Object getProperty(String property) {
    	Object propertyValue = DefaultGroovyMethods.getProperties(this).get(property);
    	if(propertyValue == null) {
    		propertyValue= InvokerHelper.getMetaClass(getAdaptee()).getProperty(getAdaptee(),property);
        }
        return propertyValue;
    }

    public void setProperty(String property, Object newValue) {
    	final Map properties = DefaultGroovyMethods.getProperties(this);
    	if(properties.containsKey(property)) {
            super.setProperty(property,newValue);
        } else {
            InvokerHelper.getMetaClass(getAdaptee()).setProperty(getAdaptee(),property,newValue);
        }
    }

    /**
     * This method allows transparent iterating over an adapted collection.
     * The previous behavior was to return the adaptee instead of the iterator()
     * when iterator()was called so this keeps that behavior for non-collections.
     * @return either an Iterator (if adaptee is a collection) or this.
     *
     
    public Object iterator() {
        if(getAdaptee() instanceof Collection) {
           return ((Collection)getAdaptee()).iterator();
        }
        else {
            return getAdaptee();
        }
    }*/  

}
