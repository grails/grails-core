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

import groovy.lang.MetaClass;
/**
 * An interface for MetaClass that adapt other MetaClasses to implement
 * 
 * @author Graeme Rocher
 *
 */
public interface AdapterMetaClass {

	/**
	 * @return the adaptee The adaptee
	 */
	public abstract MetaClass getAdaptee();
	
	/**
	 * Sets the Adaptee
	 * 
	 * @param adaptee The adaptee to set
	 */
	void setAdaptee(MetaClass adaptee);

}