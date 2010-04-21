/*
 * Copyright 2008 the original author or authors.
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
package org.codehaus.groovy.grails.cli.support;

import java.util.Map;

import org.springframework.mock.jndi.SimpleNamingContextBuilder;

/**
 * This interface is used for plugins to register additional handler for JNDI binding
 * For example the Mail plugin can add a binder for javax.mail.Session etc.
 * 
 * @author Graeme Rocher
 * @since 1.2.3
 *
 */
public interface JndiBindingHandler {

	/**
	 * @return The interface type this binder handles (eg. javax.sql.DataSource)
	 */
	String getType();
	
	/**
	 * Handles the action binding
	 * 
	 * @param builder The builder
	 * @param entryName The entry name
	 * @param entryProperties The entry properties
	 */
	void handleBinding(SimpleNamingContextBuilder builder, String entryName, Map entryProperties); 
}
