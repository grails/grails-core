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
 * <p>An interface that defines a dynamic constructor
 * 
 * @author Graeme Rocher
 * @since 0.2
 * 
 * Date Created: 9th June 2006
 */
public interface DynamicConstructor {

	/**
	 * Test whether the specified arguments match this constructor
	 * 
	 * @param args The arguments to check
	 * @return True if the arguments match
	 */
	boolean isArgumentsMatch(Object[] args);
	
	/**
	 * Invokes the dynamic constructor
	 * 
	 * @param clazz The actual class 
	 * @param args The arguments
	 * 
	 * @return The returned instance
	 */
	Object invoke(Class clazz, Object[] args);
}
