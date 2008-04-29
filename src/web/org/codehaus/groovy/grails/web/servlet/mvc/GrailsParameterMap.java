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
package org.codehaus.groovy.grails.web.servlet.mvc;

import groovy.lang.GString;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.ControllerExecutionException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * A parameter map class that allows mixing of request parameters and controller parameters. If a controller
 * parameter is set with the same name as a request parameter the controller parameter value is retrieved.
 * 
 * @author Graeme Rocher
 * @author Kate Rhodes
 * 
 * @since Oct 24, 2005
 */
public class GrailsParameterMap implements Map {

	private Map parameterMap;
	private HttpServletRequest request;

    /**
     * Creates a GrailsParameterMap populating from the given request object
     * @param request The request object
     */
    public GrailsParameterMap(HttpServletRequest request) {
		super();

		this.request = request;
		this.parameterMap = new HashMap();
        final Map requestMap = new LinkedHashMap(request.getParameterMap());
        if(request instanceof MultipartHttpServletRequest) {
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest)request;
            Map fileMap = multipartRequest.getFileMap();
            for (Iterator i = fileMap.keySet().iterator(); i.hasNext();) {
                Object fileName = i.next();
                requestMap.put(fileName, multipartRequest.getFile((String) fileName));
            }
        }
        for (Iterator it = requestMap.keySet().iterator(); it.hasNext(); ){
			String key = (String) it.next();
            Object paramValue = getParameterValue(requestMap, key);
            parameterMap.put(key, paramValue);
            processNestedKeys(request, requestMap, key, key ,parameterMap);
        }

    }

    private Object getParameterValue(Map requestMap, String key) {
        Object paramValue = requestMap.get(key);
        if(paramValue instanceof String[]) {
            String[] multiParams = (String[])paramValue;
            if(multiParams.length == 1) {
                paramValue = multiParams[0];
            }
        }
        return paramValue;
    }

    /*
     * This method builds up a multi dimensional hash structure from the parameters so that nested keys such as "book.author.name"
     * can be addressed like params['author'].name
     *
     * This also allows data binding to occur for only a subset of the properties in the parameter map
     */
    private void processNestedKeys(HttpServletRequest request, Map requestMap, String key, String nestedKey, Map nestedLevel) {
        final int nestedIndex = nestedKey.indexOf('.');
        if(nestedIndex > -1) {
            // We have at least one sub-key, so extract the first element
            // of the nested key as the prfix. In other words, if we have
            // 'nestedKey' == "a.b.c", the prefix is "a".
            final String nestedPrefix = nestedKey.substring(0, nestedIndex);

            // Let's see if we already have a value in the current map
            // for the prefix.
            Object prefixValue = nestedLevel.get(nestedPrefix);
            if(prefixValue == null) {
                // No value. So, since there is at least one sub-key,
                // we create a sub-map for this prefix.
                prefixValue = new GrailsParameterMap(new HashMap(), request);
                nestedLevel.put(nestedPrefix, prefixValue);
            }

            // If the value against the prefix is a map, then we store
            // the sub-keys in that map.
            if (prefixValue instanceof Map) {
                Map nestedMap = (Map)prefixValue;
                if(nestedIndex < nestedKey.length()-1) {
                    final String remainderOfKey = nestedKey.substring(nestedIndex + 1, nestedKey.length());
                    nestedMap.put(remainderOfKey,getParameterValue(requestMap, key) );
                    if(remainderOfKey.indexOf('.') >-1) {
                        processNestedKeys(request, requestMap,key,remainderOfKey,nestedMap);
                    }
                }
            }
        }
    }

    /**
     * This constructor does not populate the GrailsParameterMap from the request but instead uses
     * the supplied values
     *
     * @param values The values to populate with
     * @param request The request object
     */
	public GrailsParameterMap(Map values,HttpServletRequest request) {
		super();

		this.request = request;
		this.parameterMap = values;
	}

    /**
	 * @return Returns the request.
	 */
	public HttpServletRequest getRequest() {
		return request;
	}

	public int size() {
		return parameterMap.size();
	}

	public boolean isEmpty() {			
		return parameterMap.isEmpty();
	}

	public boolean containsKey(Object key) {
		return parameterMap.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return parameterMap.containsValue(value);
	}

	public Object get(Object key) {
		// removed test for String key because there 
		// should be no limitations on what you shove in or take out
		if (parameterMap.get(key) instanceof String []){
			String[] valueArray = (String[])parameterMap.get(key);
			if(valueArray == null){
				return null;
			}
			
			if(valueArray.length == 1) {
				return valueArray[0];
			}
		}
		return parameterMap.get(key);
		
	}

	public Object put(Object key, Object value) {
        if(value instanceof GString) value = value.toString();
        return parameterMap.put(key, value);
	}

	public Object remove(Object key) {
		return parameterMap.remove(key);
	}

	public void putAll(Map map) {
        for (Iterator i = map.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            put(key, map.get(key));
        }
	}

	public void clear() {
		parameterMap.clear();
	}

	public Set keySet() {
		return parameterMap.keySet();
	}

	public Collection values() {
		return parameterMap.values();
	}

	public Set entrySet() {
		return parameterMap.entrySet();
	}

    /**
     * Converts this parameter map into a query String. Note that this will flatten nested keys separating them with the
     * . character and URL encode the result
     *
     * @return A query String starting with the ? character
     */
    public String toQueryString() {

        String encoding = request.getCharacterEncoding();
        if(encoding == null) encoding = "UTF-8";
        StringBuffer queryString = new StringBuffer("?");

        for (Iterator i = entrySet().iterator(); i.hasNext();) {
            Entry entry = (Entry) i.next();
            boolean hasMore = i.hasNext();
            boolean wasAppended = appendEntry(entry, queryString, encoding, "");
            if(hasMore && wasAppended) queryString.append('&');
        }
        return queryString.toString();
    }

    private boolean appendEntry(Entry entry, StringBuffer queryString, String encoding, String path) {
        String name = entry.getKey().toString();
        if(name.indexOf(".") > -1) return false; // multi-d params handled by recursion
        Object value = entry.getValue();
        if(value == null) value = "";
        else if(value instanceof GrailsParameterMap) {
            GrailsParameterMap child = (GrailsParameterMap)value;
            Set nestedEntrySet = child.entrySet();
            for (Iterator i = nestedEntrySet.iterator(); i.hasNext();) {
                Entry childEntry = (Entry) i.next();
                appendEntry(childEntry, queryString, encoding, entry.getKey().toString()+'.');
            }
        }
        else {
            try {
                queryString.append(URLEncoder.encode(path+name, encoding))
                        .append('=')
                        .append(URLEncoder.encode(value.toString(), encoding));
            } catch (UnsupportedEncodingException e) {
                throw new ControllerExecutionException("Unable to convert parameter map ["+this+"] to a query string: " + e.getMessage(),e);
            }
        }
        return true;
    }

    public String toString() {
        return DefaultGroovyMethods.inspect(this.parameterMap);
    }
}
