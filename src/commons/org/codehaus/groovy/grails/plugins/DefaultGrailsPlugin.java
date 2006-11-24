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

import grails.spring.BeanBuilder;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.exceptions.PluginException;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.ApplicationContext;

/**
 * Implementation of the GrailsPlugin interface that wraps a Groovy plugin class
 * and provides the magic to invoke its various methods from Java
 * 
 * @author Graeme Rocher
 *
 */
public class DefaultGrailsPlugin extends AbstractGrailsPlugin implements GrailsPlugin {

	private GrailsPluginClass pluginGrailsClass;
	private GroovyObject plugin;
	protected BeanWrapper pluginBean;

	
	public DefaultGrailsPlugin(Class pluginClass, GrailsApplication application) {
		super(pluginClass, application);
		this.pluginGrailsClass = new GrailsPluginClass(pluginClass);
		this.plugin = (GroovyObject)this.pluginGrailsClass.newInstance();
		this.pluginBean = new BeanWrapperImpl(this.plugin);
		this.dependencies = Collections.EMPTY_MAP;
		if(this.pluginBean.isReadableProperty(DEPENDS_ON)) {
			this.dependencies = (Map)GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(this.plugin, DEPENDS_ON);
			this.dependencyNames = (String[])this.dependencies.keySet().toArray(new String[this.dependencies.size()]);
		}
		if(this.pluginBean.isReadableProperty(VERSION)) {
			BigDecimal bd = (BigDecimal)this.plugin.getProperty("version");
			this.version = bd;
		}
		else {
			throw new PluginException("Plugin ["+getName()+"] must specify a version!");
		}
	}

	public void doWithApplicationContext(ApplicationContext applicationContext) {
		if(this.pluginBean.isReadableProperty(DO_WITH_APPLICATION_CONTEXT)) {
			Closure c = (Closure)this.plugin.getProperty(DO_WITH_APPLICATION_CONTEXT);
			c.call(new Object[]{applicationContext});						
		}
	}

	public void doWithRuntimeConfiguration(
			RuntimeSpringConfiguration springConfig) {
		
		if(this.pluginBean.isReadableProperty(DO_WITH_SPRING)) {
			Closure c = (Closure)this.plugin.getProperty(DO_WITH_SPRING);
			BeanBuilder bb = new BeanBuilder();
			bb.setSpringConfig(springConfig);
			bb.setApplication(application);
			c.setDelegate(bb);
			bb.invokeMethod("beans", new Object[]{c});
		}

	}

	public String getName() {
		return this.pluginGrailsClass.getLogicalPropertyName();
	}

	public BigDecimal getVersion() {		
		return this.version;
	}
	public String[] getDependencyNames() {
		return this.dependencyNames;
	}

	public BigDecimal getDependentVersion(String name) {
		BigDecimal dependentVersion = (BigDecimal)this.dependencies.get(name);
		if(dependentVersion == null)
			throw new PluginException("Plugin ["+getName()+"] referenced dependency ["+name+"] with no version!");
		else 
			return dependentVersion;
	}

	public String toString() {
		return "["+getName()+":"+getVersion()+"]";
	}

}
