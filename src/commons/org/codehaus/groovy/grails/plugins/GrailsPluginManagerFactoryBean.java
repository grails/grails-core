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

import groovy.lang.GroovyClassLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A factory bean for loading the GrailsPluginManager instance
 * 
 * @author Graeme Rocher
 * @since 0.4
 *
 */
public class GrailsPluginManagerFactoryBean implements FactoryBean, InitializingBean, ApplicationContextAware {


	private GrailsApplication application;
	private GrailsPluginManager pluginManager;
    private static final Log LOG = LogFactory.getLog(GrailsPluginManagerFactoryBean.class);
    private Resource descriptor;
    private ResourceLoader resourceLoader;
    private ApplicationContext applicationContext;


    /**
	 * @param application the application to set
	 */
	public void setApplication(GrailsApplication application) {
		this.application = application;
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
            if(descriptor == null) throw new IllegalStateException("Cannot create PluginManager, /WEB-INF/grails.xml not found!");

            GroovyClassLoader classLoader = application.getClassLoader();
            List classes = new ArrayList();
            SAXReader reader = new SAXReader();
            InputStream inputStream = null;

            try {
                inputStream = descriptor.getInputStream();
                Document doc = reader.read(inputStream);
                List grailsClasses = doc.selectNodes("/grails/plugins/plugin");
                for (Iterator i = grailsClasses.iterator(); i.hasNext();) {
                    Node node = (Node) i.next();
                    final String pluginName = node.getText();
                    classes.add(classLoader.loadClass(pluginName));
                }
            } finally {
                if(inputStream!=null)
                    inputStream.close();
            }

            Class[] loadedPlugins = (Class[])classes.toArray(new Class[classes.size()]);

            pluginManager = new DefaultGrailsPluginManager(loadedPlugins, application);
            pluginManager.setApplicationContext(applicationContext);
            PluginManagerHolder.setPluginManager(pluginManager);
			pluginManager.loadPlugins();
		}
        this.pluginManager.setApplication(application);
        this.pluginManager.doArtefactConfiguration();
        application.initialise();
    }

    public void setGrailsDescriptor(Resource grailsDescriptor) {
        this.descriptor = grailsDescriptor;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
