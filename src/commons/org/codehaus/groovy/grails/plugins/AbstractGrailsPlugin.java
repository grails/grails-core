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
package org.codehaus.groovy.grails.plugins;

import groovy.util.slurpersupport.GPathResult;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.groovy.grails.commons.AbstractGrailsClass;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.springframework.context.ApplicationContext;
/**
 * Abstract implementation that provides some default behaviours
 * 
 * @author Graeme Rocher
 *
 */
public abstract class AbstractGrailsPlugin implements GrailsPlugin {



	protected GrailsApplication application;
	protected BigDecimal version = new BigDecimal("0.1");
	protected Map dependencies = new HashMap();
	protected String[] dependencyNames = new String[0];
	protected Class pluginClass;

	/**
	 * Wrapper Grails class for plugins
	 * 
	 * @author Graeme Rocher
	 *
	 */
	class GrailsPluginClass extends AbstractGrailsClass {
		public GrailsPluginClass(Class clazz) {
			super(clazz, TRAILING_NAME);
		}
		
	}
	public AbstractGrailsPlugin(Class pluginClass, GrailsApplication application) {
		if(pluginClass == null) {
			throw new IllegalArgumentException("Argument [pluginClass] cannot be null");
		}
		if(!pluginClass.getName().endsWith(TRAILING_NAME)) {
			throw new IllegalArgumentException("Argument [pluginClass] with value ["+pluginClass+"] is not a Grails plugin (class name must end with 'GrailsPlugin')");
		}
		this.application = application;
		this.pluginClass = pluginClass;
	}
	public abstract void doWithApplicationContext(ApplicationContext applicationContext);
	

	public abstract void doWithRuntimeConfiguration(RuntimeSpringConfiguration springConfig);
	
	
	public void checkForChanges() {
		// do nothing		
	}

	public void doWithWebDescriptor(GPathResult webXml) {
		// do nothing		
	}
	public String[] getDependencyNames() {
		return this.dependencyNames;
	}

	public BigDecimal getDependentVersion(String name) {
		return null;
	}

	public String getName() {
		return pluginClass.getName();
	}

	public BigDecimal getVersion() {
		return this.version;
	}
	
}
