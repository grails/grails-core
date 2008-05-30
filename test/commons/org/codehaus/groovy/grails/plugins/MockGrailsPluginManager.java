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
import junit.framework.Assert;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.exceptions.PluginException;
import org.springframework.core.io.Resource;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/**
 * @author Graeme Rocher
 * @since 0.4
 *
 */

public class MockGrailsPluginManager extends AbstractGrailsPluginManager {
    private ServletContext servletContext;
    private boolean checkForChangesExpected = false;

    public ServletContext getServletContext() {
        return servletContext;
    }

    public MockGrailsPluginManager(GrailsApplication application) {
		super(application);
		loadPlugins();
	}
	
	public MockGrailsPluginManager() {
		this(new DefaultGrailsApplication(new Class[0], new GroovyClassLoader()));
	}
	
	public GrailsPlugin getGrailsPlugin(String name) {
		return (GrailsPlugin)this.plugins.get(name);
	}

	public GrailsPlugin getGrailsPlugin(String name, BigDecimal version) {
		return (GrailsPlugin)this.plugins.get(name);
	}

	public boolean hasGrailsPlugin(String name) {
		return this.plugins.containsKey(name);
	}
	
	public void registerMockPlugin(GrailsPlugin plugin) {
		this.plugins.put(plugin.getName(), plugin);
		this.pluginList.add(plugin);
	}

	public void loadPlugins() throws PluginException {
		this.initialised = true;
	}

	public void checkForChanges() {
        Assert.assertTrue(this.checkForChangesExpected);
        this.checkForChangesExpected = false;
    }

	public void doWebDescriptor(Resource descriptor, Writer target) {
		// do nothing
	}

	public void doWebDescriptor(File descriptor, Writer target) {
		// do nothing
	}

	public boolean isInitialised() {
		return true;
	}

    public void refreshPlugin(String name) {
        GrailsPlugin plugin = (GrailsPlugin)plugins.get(name);
        if(plugin != null) {
            plugin.refresh();
        }
    }

    public Collection getPluginObservers(GrailsPlugin plugin) {
        throw new UnsupportedOperationException("The class [MockGrailsPluginManager] doesn't support the method getPluginObservers");
    }

    public void informObservers(String pluginName, Map event) {
        // do nothing
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void expectCheckForChanges() {
        Assert.assertFalse(this.checkForChangesExpected);
        this.checkForChangesExpected = true;
    }

    public void verify() {
        Assert.assertFalse(this.checkForChangesExpected);
    }
}
