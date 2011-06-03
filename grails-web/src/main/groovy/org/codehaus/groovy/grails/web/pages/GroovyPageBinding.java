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
package org.codehaus.groovy.grails.web.pages;

import groovy.lang.Binding;
import groovy.lang.MetaProperty;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;

import java.util.Map;

/**
 * Script Binding that is used in GSP evaluation
 * 
 * 
 * @author Lari Hotari
 *
 */
public class GroovyPageBinding extends Binding {
	private static final Log log = LogFactory.getLog(GroovyPageBinding.class);
    private String pluginContextPath;
    private GrailsPlugin plugin;
    private Binding parent;

    public GroovyPageBinding() {
        super();
    }
    
    public GroovyPageBinding(String pluginContextPath) {
    	setPluginContextPath(pluginContextPath);
    }

    @SuppressWarnings("rawtypes")
    public GroovyPageBinding(Map variables) {
        super(variables);
    }

    public GroovyPageBinding(String[] args) {
        super(args);
    }

    @Override
    public Object getProperty(String property) {
        return getVariable(property);
    }

	@Override
	public Object getVariable(String name) {
		Object val = getVariables().get(name);
		if(val == null && !getVariables().containsKey(name)) {
			if(GroovyPage.PAGE_SCOPE.equals(name)) return this;
			if(parent != null) {
				val = parent.getVariable(name);
				if(val != null) {
					// cache variable in this context since parent context cannot change during usage of this context
					setVariable(name, val);
				}
			}
			if(val==null) {
				MetaProperty metaProperty = getMetaClass().getMetaProperty(name);
				if(metaProperty != null) {
					val = metaProperty.getProperty(this);
				}
			}
		}
		return val;
	}    

    @Override
    public void setProperty(String property, Object newValue) {
        setVariable(property, newValue);
    }
    
	@Override
	public void setVariable(String name, Object value) {
		if(!GroovyPage.isReservedName(name)) {
			super.setVariable(name, value);
		} else {
			if(log.isWarnEnabled()) {
				log.warn("Cannot override reserved variable '" + name + "'");
			}
		}
	}    
    
    public String getPluginContextPath() {
        return pluginContextPath;
    }

    public void setPluginContextPath(String pluginContextPath) {
        this.pluginContextPath = pluginContextPath;
        setVariable("pluginContextPath", pluginContextPath);
    }

    public void setPagePlugin(GrailsPlugin plugin) {
        this.plugin = plugin;
        setVariable("pagePlugin", plugin);
    }

    public GrailsPlugin getPagePlugin() {
        return plugin;
    }

	public Binding getParent() {
		return parent;
	}

	public void setParent(Binding parent) {
		this.parent = parent;
	}


}