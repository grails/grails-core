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

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

/**
 * A factory bean for loading the GrailsPluginManager instance
 * 
 * @author Graeme Rocher
 * @since 0.4
 *
 */
public class GrailsPluginManagerFactoryBean implements FactoryBean, InitializingBean{

	private Resource[] pluginFiles = new Resource[0];
	private GrailsApplication application;
	private GrailsPluginManager pluginManager;
	
	
	/**
	 * @param application the application to set
	 */
	public void setApplication(GrailsApplication application) {
		this.application = application;
	}

	/**
	 * @param pluginFiles the pluginFiles to set
	 */
	public void setPluginFiles(Resource[] pluginFiles) {
		this.pluginFiles = pluginFiles;
	}

	public Object getObject() throws Exception {
		return this.pluginManager;
	}

	public Class getObjectType() {
		return GrailsPluginManager.class;
	}

	public boolean isSingleton() {
		return true;
	}

	public void afterPropertiesSet() throws Exception {
		this.pluginManager = PluginManagerHolder.getPluginManager();

		if(pluginManager == null) {
			pluginManager = new DefaultGrailsPluginManager(pluginFiles, application);
			PluginManagerHolder.setPluginManager(pluginManager);
			pluginManager.loadPlugins();
		}
        this.pluginManager.setApplication(application);
        this.pluginManager.doArtefactConfiguration();
        application.initialise();
    }

}
