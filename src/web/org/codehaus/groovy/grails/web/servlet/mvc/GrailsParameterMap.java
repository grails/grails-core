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

import java.util.Collection;
import java.util.HashMap;
//import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
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
		
	public GrailsParameterMap(HttpServletRequest request) {
		super();

		this.request = request;
		this.parameterMap = new HashMap();
		for (Iterator it = request.getParameterMap().keySet().iterator(); it.hasNext(); ){
			Object key = it.next();
			parameterMap.put(key, request.getParameterMap().get(key));
		}
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
		return parameterMap.put(key, value);
	}

	public Object remove(Object key) {
		return parameterMap.remove(key);
	}

	public void putAll(Map map) {
		parameterMap.putAll(map);
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

}
