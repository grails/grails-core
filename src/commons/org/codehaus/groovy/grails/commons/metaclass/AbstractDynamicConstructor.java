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
/**
 * <p>Abstract class that provides default implementation for isArgumentsMatch
 * 
 * @author Graeme Rocher
 * @since 0.2
 * 
 * Date Created: 9th June 2006
 */

public abstract class AbstractDynamicConstructor implements DynamicConstructor {

	private Class[] argumentTypes;

	/**
	 * Takes an array of types required to match this constructor
	 * 
	 * @param argumentTypes The argument types
	 */
	public AbstractDynamicConstructor(Class[] argumentTypes) {
		this.argumentTypes = argumentTypes;
	}
	
	/**
	 * @return True if the arguments types match those specified in the constructor
	 */
	public boolean isArgumentsMatch(Object[] args) {
		if(args.length != argumentTypes.length)
			return false;
		else {
			for (int i = 0; i < args.length; i++) {
				if(!args[i].getClass().equals(argumentTypes[i]))
					return false;
			}
		}
		return true;
	}

	public abstract Object invoke(Class clazz, Object[] args);

}
