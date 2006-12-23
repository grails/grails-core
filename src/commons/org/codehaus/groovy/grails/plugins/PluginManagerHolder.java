/*
 * Copyright 2004-2006 Graeme Rocher
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
package org.codehaus.groovy.grails.plugins;

/**
 * Manages a thread bound plugin manager instance
 * 
 * @author Graeme Rocher
 * @since 0.4
 *
 */
public abstract class PluginManagerHolder {

	private static final ThreadLocal pluginManagerHolder = new InheritableThreadLocal();
	
	/**
	 * Bind the given GrailsPluginManager instance to the current Thread
	 * @param pluginManager The GrailsPluginManager to expose
	 */
	public static void setPluginManager(GrailsPluginManager pluginManager) {
		pluginManagerHolder.set(pluginManager);
	}
	
	/**
	 * Retrieves the GrailsPluginManager bound to the current Thread 
	 * @return The GrailsPluginManager or null
	 */
	public static GrailsPluginManager getPluginManager() {
		return (GrailsPluginManager)pluginManagerHolder.get();
	}
	
	/**
	 * Retrieves the bound GrailsPluginManager that resides in the current Thread
	 * @return The GrailsPluginManager 
	 * @throws IllegalStateException When there is no bound GrailsPluginManager
	 */
	public static GrailsPluginManager currentPluginManager() {
		GrailsPluginManager current = getPluginManager();
		if(current == null)
			throw new IllegalStateException("No thread-bound PluginManager");
		return current;
	}
}
