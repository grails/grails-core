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

import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import org.apache.commons.collections.CollectionUtils;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        return bindObjectToInstance(object, source, Collections.EMPTY_LIST,Collections.EMPTY_LIST, null);
    }

    /**
     * Binds the given source object to the given target object performing type conversion if necessary
     *
     * @param object The object to bind to
     * @param source The source object
     * @param include The list of properties to include
     * @param exclude The list of properties to exclude
     *
     * @return A BindingResult or null if it wasn't successful
     */
    public static BindingResult bindObjectToInstance(Object object, Object source, List include, List exclude, String filter) {
        BindingResult bindingResult = null;
        if(source instanceof GrailsParameterMap) {
			GrailsParameterMap parameterMap = (GrailsParameterMap)source;
			HttpServletRequest request = parameterMap.getRequest();
			GrailsDataBinder dataBinder = createDataBinder(object, include, exclude, request);
			dataBinder.bind(parameterMap);
            bindingResult = dataBinder.getBindingResult();
        }
        else if (source instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest)source;
            GrailsDataBinder dataBinder = createDataBinder(object, include, exclude, request);
            dataBinder.bind(request);
            performBindFromRequest(dataBinder, request,filter);
            bindingResult = dataBinder.getBindingResult();
        }
        else if(source instanceof Map) {
			Map propertyMap = (Map)source;
            propertyMap = convertPotentialGStrings(propertyMap);
            GrailsDataBinder binder = createDataBinder(object, include, exclude, null);
            
            performBindFromPropertyValues(binder, new MutablePropertyValues(propertyMap),filter);
            bindingResult = binder.getBindingResult();
        }
        else {
        	GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.getRequestAttributes();
            if(webRequest != null) {
                GrailsDataBinder binder = createDataBinder(object, include, exclude, webRequest.getCurrentRequest());
                HttpServletRequest request = webRequest.getCurrentRequest();
                performBindFromRequest(binder, request,filter);
            }


        }
        MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(object.getClass());
        if(mc.hasProperty(object, "errors")!=null && bindingResult!=null) {
            mc.setProperty(object,"errors", bindingResult);
        }
        return bindingResult;
    }

    private static void performBindFromPropertyValues(GrailsDataBinder binder, MutablePropertyValues mutablePropertyValues, String filter) {
        if(filter!=null) {
            binder.bind(mutablePropertyValues,filter);
        }
        else {
            binder.bind(mutablePropertyValues);
        }
    }

    private static void performBindFromRequest(GrailsDataBinder binder, HttpServletRequest request,String filter) {
        if(filter!=null) {
            binder.bind(request,filter);
        }
        else {
            binder.bind(request);
        }
    }

    private static GrailsDataBinder createDataBinder(Object object, List include, List exclude, HttpServletRequest request) {

        GrailsDataBinder binder;
        if(request!=null) {
            binder = GrailsDataBinder.createBinder(object, object.getClass().getName(), request);
        }
        else {
            binder = GrailsDataBinder.createBinder(object, object.getClass().getName());            
        }
        includeExcludeFields(binder, include, exclude);
        return binder;
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

    private static void includeExcludeFields(GrailsDataBinder dataBinder, List allowed, List disallowed) {
        updateAllowed( dataBinder, allowed);
        updateDisallowed( dataBinder, disallowed);
    }

    private static void updateAllowed(GrailsDataBinder binder, List allowed) {
          if (allowed != null) {
            String[] currentAllowed = binder.getAllowedFields();
            List newAllowed = new ArrayList(allowed);
            CollectionUtils.addAll( newAllowed, currentAllowed);
            String[] value = new String[newAllowed.size()];
            newAllowed.toArray(value);
            binder.setAllowedFields(value);
        }
    }

    private static void updateDisallowed( GrailsDataBinder binder, List disallowed) {
        if (disallowed != null) {
            String[] currentDisallowed = binder.getDisallowedFields();
            List newDisallowed = new ArrayList(disallowed);
            CollectionUtils.addAll( newDisallowed, currentDisallowed);
            String[] value = new String[newDisallowed.size()];
            newDisallowed.toArray(value);
            binder.setDisallowedFields(value);
        }
    }
}
