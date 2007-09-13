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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.metaclass.DynamicConstructor;
import org.codehaus.groovy.grails.exceptions.GrailsDomainException;
import org.codehaus.groovy.grails.web.binding.GrailsDataBinder;
import org.codehaus.groovy.grails.web.binding.DataBindingUtils;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValues;
import org.springframework.validation.DataBinder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.context.ApplicationContext;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
/**
 * A dynamic property that uses a Map of OGNL expressions to sets properties on the target object
 * 
 * @author Graeme Rocher
 * @since 0.3
 * 
 * Created 23/07/06
 */

public class DataBindingDynamicConstructor implements DynamicConstructor {
	private static final Log LOG = LogFactory.getLog( DataBindingDynamicConstructor.class );
    private ApplicationContext applicationContext;

    public DataBindingDynamicConstructor() {
    }

    public DataBindingDynamicConstructor(ApplicationContext ctx) {
        this.applicationContext = ctx;
    }

    public boolean isArgumentsMatch(Object[] args) {
        if(args.length == 0) return true;
        if(args.length > 1 ) return false;        
        if(args.length == 1) {
            if(HttpServletRequest.class.isAssignableFrom(args[0].getClass())) return true;
            if(Map.class.isAssignableFrom(args[0].getClass())) return true;
        }
		return false;
	}

	public Object invoke(Class clazz, Object[] args) {
		Object map = args.length > 0 ? args[0] : null;
        Object instance;
        if(applicationContext!=null && applicationContext.containsBean(clazz.getName())) {
            instance = applicationContext.getBean(clazz.getName());
        }
        else {

            try {
                instance = clazz.newInstance();
            } catch (InstantiationException e1) {
                throw new GrailsDomainException("Error instantiated class [" + clazz + "]: " + e1.getMessage(),e1);
            } catch (IllegalAccessException e1) {
                throw new GrailsDomainException("Illegal access instantiated class [" + clazz + "]: " + e1.getMessage(),e1);
            }
        }


        if (map !=null) {
                DataBindingUtils.bindObjectToInstance(instance, map);
        }
        return instance;
	}


}
