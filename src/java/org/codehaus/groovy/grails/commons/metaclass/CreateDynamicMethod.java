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
package org.codehaus.groovy.grails.commons.metaclass;

import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>A dynamic static method that is a factory method for creating new instances
 * 
 * eg. Account.create()
 * 
 * @author Graeme Rocher
 * @since Oct 24, 2005
 */
public class CreateDynamicMethod extends AbstractStaticMethodInvocation  {
	public static final String METHOD_NAME = "create";
	private static final Pattern METHOD_PATTERN = Pattern.compile("^create$");
	private static final Log LOG = LogFactory.getLog(CreateDynamicMethod.class);
	
	
	public CreateDynamicMethod() {
		super();
		setPattern(METHOD_PATTERN);
	}

	public Object invoke(Class clazz, String methodName, Object[] arguments) {		
		try {
			return clazz.newInstance();
		} catch (InstantiationException e) {
			LOG.warn("Unable to instantiate class ["+clazz.getName()+"]: " + e.getMessage());
		} catch (IllegalAccessException e) {
			LOG.warn("Illegal access instantiating class ["+clazz.getName()+"]: " + e.getMessage());
		}		
		return null;
	}

}
