/* Copyright 2006-2007 Graeme Rocher
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
package org.codehaus.groovy.grails.web.binding;

import org.springframework.validation.BindingResult;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.beans.MutablePropertyValues;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import groovy.lang.MetaClass;
import groovy.lang.GroovySystem;

/**
 * Utility methods to perform data binding from Grails objects
 *
 * @author Graeme Rocher
 * @since 1.0
 *
 *        <p/>
 *        Created: Sep 13, 2007
 *        Time: 2:34:11 PM
 */
public class DataBindingUtils {

    /**
     * Binds the given source object to the given target object performing type conversion if necessary
     *
     * @param object The object to bind to
     * @param source The source object
     * @return A BindingResult or null if it wasn't successful
     */
    public static BindingResult bindObjectToInstance(Object object, Object source) {
        BindingResult bindingResult = null;
        if(source instanceof GrailsParameterMap) {
			GrailsParameterMap parameterMap = (GrailsParameterMap)source;
			HttpServletRequest request = parameterMap.getRequest();
			GrailsDataBinder dataBinder = GrailsDataBinder.createBinder(object, object.getClass().getName(),request);
			dataBinder.bind(parameterMap);
            bindingResult = dataBinder.getBindingResult();
        }
        else if (source instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest)source;
            ServletRequestDataBinder dataBinder = GrailsDataBinder.createBinder(object, object.getClass().getName(),request);
            dataBinder.bind(request);
            bindingResult = dataBinder.getBindingResult();
        }
        else if(source instanceof Map) {

			Map propertyMap = (Map)source;
            propertyMap = convertPotentialGStrings(propertyMap);
            GrailsDataBinder binder = GrailsDataBinder.createBinder(object, object.getClass().getName());
            binder.bind(new MutablePropertyValues(propertyMap));
            bindingResult = binder.getBindingResult();
        }
        MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(object.getClass());
        if(mc.hasProperty(object, "errors")!=null && bindingResult!=null) {
            mc.setProperty(object,"errors", bindingResult);
        }
        return bindingResult;
    }

	private static Map convertPotentialGStrings(Map args) {
		Map newArgs = new java.util.HashMap();
		for(java.util.Iterator i = args.keySet().iterator(); i.hasNext();) {
			Object key = i.next();
			Object value = args.get(key);
			if(key instanceof groovy.lang.GString) {
				key = key.toString();
			}
			if(value instanceof groovy.lang.GString) {
				value = value.toString();
			}
			newArgs.put(key,value);
		}
		return newArgs;
	}
}
