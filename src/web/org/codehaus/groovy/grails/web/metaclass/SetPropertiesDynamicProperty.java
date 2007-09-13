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
package org.codehaus.groovy.grails.web.metaclass;

import groovy.lang.MissingPropertyException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicProperty;
import org.codehaus.groovy.grails.web.binding.DataBindingUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.validation.BindingResult;

/**
 * A dynamic property that uses a Map of OGNL expressions to sets properties on the target object
 * 
 * @author Graeme Rocher
 * @since Oct 24, 2005
 */
public class SetPropertiesDynamicProperty extends AbstractDynamicProperty {

	private static final Log LOG = LogFactory.getLog( SetPropertiesDynamicProperty.class );
	
	private static final String PROPERTY_NAME = "properties";
    private TypeConverter converter = new SimpleTypeConverter();

    public SetPropertiesDynamicProperty() {
		super(PROPERTY_NAME);
	}

	/**
	 * @return A org.apache.commons.beanutils.BeanMap instance
	 */
	public Object get(Object object) {				
		return DefaultGroovyMethods.getProperties(object);
	}

	/**
	 * Sets the property on the specified object with the specified value. The
	 * value is expected to be a Map containing OGNL expressions for the keys
	 * and objects for the values.
	 * 
	 * @param object The target object
	 * @param newValue The value to set
	 */
	public void set(Object object, Object newValue) {
		if(newValue == null)
			return;

        BindingResult result = DataBindingUtils.bindObjectToInstance(object, newValue);
        if(result == null)
            throw new MissingPropertyException(PROPERTY_NAME,object.getClass());
    }

}
