/**
 * 
 */
package org.codehaus.groovy.grails.web.pages;

import groovy.lang.Binding;

import java.util.Map;

public class GroovyPageBinding extends Binding {
    private String pluginContextPath;

    public GroovyPageBinding() {
		super();
	}

	public GroovyPageBinding(Map variables) {
		super(variables);
	}

	public GroovyPageBinding(String[] args) {
		super(args);
	}

    public GroovyPageBinding(String pluginContextPath) {
        this.pluginContextPath = pluginContextPath;
    }

    public String getPluginContextPath() {
        return pluginContextPath;
    }

    public void setPluginContextPath(String pluginContextPath) {
        this.pluginContextPath = pluginContextPath;
    }

    @Override
	public Object getProperty(String property) {
        if(getMetaClass().hasProperty(this, property)!=null) {
            return getMetaClass().getProperty(this, property);
        }
		return getVariable(property);
	}

	@Override
	public void setProperty(String property, Object newValue) {
		setVariable(property, newValue);
	}
}