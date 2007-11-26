/*
 * Copyright 2004-2007 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.exceptions.PluginException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Class with responsibility for loading core plugin classes. 
 * Contains functionality moved in from <code>DefaultGrailsPluginManager</code>
 * @author Graeme Rocher
 * @author Phil Zoio
 */
public class CorePluginFinder {

	//This class contains functionality originally found in 
	//DefaultGrailsPluginManager, but moved after 0.5.6
	
	private static final Log LOG = LogFactory.getLog(CorePluginFinder.class);

	private final PathMatchingResourcePatternResolver resolver;

	private final GrailsApplication application;

	private final Set foundPluginClasses;

	public CorePluginFinder(GrailsApplication application) {
		super();
		this.resolver = new PathMatchingResourcePatternResolver();
		this.application = application;
		this.foundPluginClasses = new HashSet();
	}

	public Set getPluginClasses() {

		// just in case we try to use this twice
		foundPluginClasses.clear();

		try {
			Resource[] resources = resolver
					.getResources("classpath*:org/codehaus/groovy/grails/**/plugins/**/*GrailsPlugin.class");
			if (resources.length > 0) {
				loadCorePluginsFromResources(resources);
			} else {
				LOG.warn("WARNING: Grails was unable to load core plugins dynamically. This is normally a problem with the container class loader configuration, see troubleshooting and FAQ for more info. ");
				loadCorePluginsStatically();
			}
		} catch (IOException e) {
			throw new PluginException(
					"I/O exception configuring core plug-ins: "
							+ e.getMessage(), e);
		}
		return foundPluginClasses;
	}

	private void loadCorePluginsStatically() {

		// This is a horrible hard coded hack, but there seems to be no way to
		// resolve .class files dynamically
		// on OC4J. If anyones knows how to fix this shout
		loadCorePlugin("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.LoggingGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.CodecsGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin");
		loadCorePlugin("org.codehaus.groovy.grails.plugins.datasource.DataSourceGrailsPlugin");
		loadCorePlugin("org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin");
		loadCorePlugin("org.codehaus.groovy.grails.plugins.web.ServletsGrailsPlugin");
		loadCorePlugin("org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.web.mapping.UrlMappingsGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.web.filters.FiltersGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.web.mimes.MimeTypesGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.webflow.WebFlowGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.orm.hibernate.HibernateGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.services.ServicesGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.converters.ConvertersGrailsPlugin");
        loadCorePlugin("org.codehaus.groovy.grails.plugins.scaffolding.ScaffoldingGrailsPlugin");
    }

	private void loadCorePluginsFromResources(Resource[] resources)
			throws IOException {

		LOG.debug("Attempting to load [" + resources.length + "] core plugins");
		for (int i = 0; i < resources.length; i++) {
			Resource resource = resources[i];
			String url = resource.getURL().toString();
			int packageIndex = url.indexOf("org/codehaus/groovy/grails");
			url = url.substring(packageIndex, url.length());
			url = url.substring(0, url.length() - 6);
			String className = url.replace('/', '.');

			loadCorePlugin(className);
		}
	}

	private Class attemptCorePluginClassLoad(String pluginClassName) {
		try {
			return application.getClassLoader().loadClass(pluginClassName);
		} catch (ClassNotFoundException e) {
			LOG.warn("[GrailsPluginManager] Core plugin [" + pluginClassName
					+ "] not found, resuming load without..");
			if (LOG.isDebugEnabled())
				LOG.debug(e.getMessage(), e);
		}
		return null;
	}

	private void loadCorePlugin(String pluginClassName) {
		Class pluginClass = attemptCorePluginClassLoad(pluginClassName);

		if (pluginClass != null) {
			addPlugin(pluginClass);
		}
	}

	private void addPlugin(Class plugin) {
		foundPluginClasses.add(plugin);
	}

}
