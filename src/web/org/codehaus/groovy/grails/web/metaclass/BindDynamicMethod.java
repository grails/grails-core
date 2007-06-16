/* Copyright 2004-2005 the original author or authors.
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

import groovy.lang.MissingMethodException;
import org.apache.commons.collections.CollectionUtils;
import org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethodInvocation;
import org.codehaus.groovy.grails.web.binding.GrailsDataBinder;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValues;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A dynamic method present in controllers allowing data binding from a map to a target instance. Example:
 *
 * <code>
 *         def a = new Account()
 *         bindData( a, this.params )
 * </code>
 *
 * @author Graeme Rocher
 * @since 10-Jan-2006
 */
public class BindDynamicMethod extends AbstractDynamicMethodInvocation {
    public static final String METHOD_SIGNATURE = "bindData";
    public static final Pattern METHOD_PATTERN = Pattern.compile('^'+METHOD_SIGNATURE+'$');

    public BindDynamicMethod() {
        super(METHOD_PATTERN);
    }


    public Object invoke(Object target, String methodName, Object[] arguments) {
        if(arguments.length < 2 || arguments.length > 4)
            throw new MissingMethodException(METHOD_SIGNATURE, target.getClass(), arguments);
        if(arguments[0] == null)
            throw new IllegalArgumentException("Argument [target] is required by method [bindData] on class ["+target.getClass()+"]");

        Object targetObject = arguments[0];
        Object bindParams = arguments[1];
        List disallowed = null;
        String filter = null;
        switch(arguments.length){
            case 3:
                if(arguments[2] instanceof String){
                    filter = (String) arguments[2];
                }else if(!(arguments[2]  instanceof List)) {
                       throw new IllegalArgumentException("The 3rd Argument for method bindData must represent disallowed properties " +
                               "and implement the interface java.util.List or be a String and represent a prefix to filter parameters with");
                }else {
                    disallowed = (List) arguments[2];
                }
                break;
            case 4:
                if(!( arguments[2] instanceof List)) {
                    throw new IllegalArgumentException("Argument [disallowed] for method [bindData] must implement the interface [java.util.List]");
                }
                disallowed = (List) arguments[2];
                if(!(arguments[3] instanceof String)) {
                    throw new IllegalArgumentException("Argument [prefix] for method [bindData] must be a String");
                 }
                filter = (String) arguments[3];
                break;
        }

        GrailsDataBinder dataBinder;
        if(bindParams instanceof GrailsParameterMap) {
            GrailsParameterMap parameterMap = (GrailsParameterMap)bindParams;
            HttpServletRequest request = parameterMap.getRequest();
            dataBinder = GrailsDataBinder.createBinder(targetObject, targetObject.getClass().getName(), request);
            updateDisallowed( dataBinder, disallowed);
            dataBinder.bind(request, filter);
        }
        else if(bindParams instanceof HttpServletRequest) {
        	GrailsWebRequest webRequest = (GrailsWebRequest)RequestContextHolder.currentRequestAttributes();
            dataBinder = GrailsDataBinder.createBinder(targetObject, targetObject.getClass().getName(),webRequest.getCurrentRequest());
            updateDisallowed( dataBinder, disallowed);
            dataBinder.bind((HttpServletRequest)bindParams, filter);
        }
        else if(bindParams instanceof Map) {
            dataBinder = new GrailsDataBinder(targetObject, targetObject.getClass().getName());
            PropertyValues pv = new MutablePropertyValues((Map)bindParams);
            updateDisallowed( dataBinder, disallowed);
            dataBinder.bind(pv, filter);
        }
        else {
        	GrailsWebRequest webRequest = (GrailsWebRequest)RequestContextHolder.currentRequestAttributes();        	
            dataBinder = GrailsDataBinder.createBinder(targetObject, targetObject.getClass().getName(), webRequest.getCurrentRequest());
            updateDisallowed( dataBinder, disallowed);
            dataBinder.bind(webRequest.getCurrentRequest(), filter);
        }
        return targetObject;
    }

    private void updateDisallowed( GrailsDataBinder binder, List disallowed) {
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
