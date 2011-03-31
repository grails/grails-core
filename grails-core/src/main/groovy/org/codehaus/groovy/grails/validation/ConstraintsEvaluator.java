/* Copyright (C) 2011 SpringSource
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
package org.codehaus.groovy.grails.validation;

import java.util.Map;

import org.codehaus.groovy.grails.commons.GrailsDomainClass;

/**
 * Evaluates and returns constraints
 * 
 * @author Graeme Rocher
 * @since 1.4
 *
 */
public interface ConstraintsEvaluator {
	
    public static final String PROPERTY_NAME = "constraints";
    public static final String CONSTRAINTS_GROOVY_SCRIPT = "Constraints.groovy";
	
	/**
	 * The default constraints to use
	 * @return A map of default constraints
	 */
	Map<String, Object> getDefaultConstraints();
	
	/**
	 * Evaluate constraints for the given class
	 * 
	 * @param cls The class to evaluate constraints for
	 * @return A map of constrained properties
	 */
	public Map<String, ConstrainedProperty> evaluate(Class cls);

	/**
	 * Evaluate constraints for the given class
	 * 
	 * @param cls The class to evaluate constraints for
	 * @return A map of constrained properties
	 */	
	public Map<String, ConstrainedProperty> evaluate(GrailsDomainClass cls);

}
