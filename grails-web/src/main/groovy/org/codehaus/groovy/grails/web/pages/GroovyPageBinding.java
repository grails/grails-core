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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Script Binding that is used in GSP evaluation
 * 
 * 
 * @author Lari Hotari
 *
 */
public class GroovyPageBinding extends Binding {
	private static final Log log = LogFactory.getLog(GroovyPageBinding.class);
    private Binding parent;
    private GroovyPage owner;
    private Set<String> cachedParentVariableNames=new HashSet<String>();

    public GroovyPageBinding() {
        super();
    }
    
    public GroovyPageBinding(Binding parent) {
    	super();
    	setParent(parent);
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

	@SuppressWarnings("unchecked")
	@Override
	public Object getVariable(String name) {
		Object val = getVariables().get(name);
		if(val == null && !getVariables().containsKey(name)) {
			if(GroovyPage.PAGE_SCOPE.equals(name)) return this;
			if(parent != null) {
				val = parent.getVariable(name);
				if(val != null) {
					// cache variable in this context since parent context cannot change during usage of this context
					getVariables().put(name, val);
					cachedParentVariableNames.add(name);
				}
			}
			// stackover flow if pluginContextPath or pagePlugin is checked by MetaProperty
			if(val==null && !name.equals(GroovyPage.PLUGIN_CONTEXT_PATH) && !name.equals("pagePlugin")) {
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
    
    /**
     * 
     * ModifyOurScopeWithBodyTagTests breaks if variable isn't changed in the binding it exists in
     * 
     * @param name
     * @return
     */
    private Binding findBindingForVariable(String name) {
    	if(cachedParentVariableNames.contains(name)) {
    		return parent;
    	} else if(getVariables().containsKey(name)) {
    		return this;
    	} else if (parent instanceof GroovyPageBinding) {
    		return ((GroovyPageBinding)parent).findBindingForVariable(name);
    	} else if (parent != null && parent.getVariables().containsKey(name)) {
    		return parent;
    	} else {
    		return null;
    	}
    }
    
	@Override
	public void setVariable(String name, Object value) {
		internalSetVariable(null, name, value);
	}
	
	@SuppressWarnings("unchecked")
	public void setVariableDirectly(String name, Object value) {
		getVariables().put(name, value);
	}

	@SuppressWarnings("unchecked")
	private void internalSetVariable(Binding bindingToUse, String name, Object value) {
		if(!GroovyPage.isReservedName(name)) {
			if(bindingToUse == null) {
				bindingToUse = findBindingForVariable(name);
				if(bindingToUse==null || bindingToUse instanceof GroovyPageRequestBinding) {
					bindingToUse=this;
				}
			}
			bindingToUse.getVariables().put(name, value);
			if(bindingToUse!=this && cachedParentVariableNames.contains(name)) {
				// maintain cached value
				getVariables().put(name, value);
			}
		} else {
			if(log.isWarnEnabled()) {
				log.warn("Cannot override reserved variable '" + name + "'");
			}
		}
	}    
    
    public String getPluginContextPath() {
        return (String)getVariable(GroovyPage.PLUGIN_CONTEXT_PATH);
    }

    @SuppressWarnings("unchecked")
	public void setPluginContextPath(String pluginContextPath) {
    	getVariables().put(GroovyPage.PLUGIN_CONTEXT_PATH, pluginContextPath);
    }

    @SuppressWarnings("unchecked")
    public void setPagePlugin(GrailsPlugin plugin) {
    	getVariables().put("pagePlugin", plugin);
    }

    public GrailsPlugin getPagePlugin() {
        return (GrailsPlugin)getVariable("pagePlugin");
    }

	public Binding getParent() {
		return parent;
	}

	public void setParent(Binding parent) {
		this.parent = parent;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void addMap(Map additionalBinding) {
    	for(Iterator<Map.Entry> i=additionalBinding.entrySet().iterator();i.hasNext();) {
    		Map.Entry entry=i.next();
    		String name = String.valueOf(entry.getKey());
    		Object value = entry.getValue();
    		internalSetVariable(this, name, value);
    	}
	}

	public GroovyPage getOwner() {
		return owner;
	}

	public void setOwner(GroovyPage owner) {
		this.owner = owner;
	}
}