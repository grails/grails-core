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
import groovy.lang.MetaClass;

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
public class ExtendedProxy extends Proxy {
    private Map propertyMap;

    public ExtendedProxy() {
        super();
        this.propertyMap = DefaultGroovyMethods.getProperties(this);
    }

    public Object getProperty(String property) {
    	Object propertyValue = propertyMap.get(property);
    	if(propertyValue == null) {
    		propertyValue= InvokerHelper.getMetaClass(getAdaptee()).getProperty(getAdaptee(),property);
        }
        return propertyValue;
    }

    public void setProperty(String property, Object newValue) {

    	if(propertyMap.containsKey(property)) {
            super.setProperty(property,newValue);
        } else {
            InvokerHelper.getMetaClass(getAdaptee()).setProperty(getAdaptee(),property,newValue);
        }
    }

    public void setMetaClass(MetaClass metaClass) {
        super.setMetaClass(metaClass);
        this.propertyMap = DefaultGroovyMethods.getProperties(this);
    }
}
