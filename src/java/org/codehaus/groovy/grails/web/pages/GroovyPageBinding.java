/**
 * 
 */
package org.codehaus.groovy.grails.web.pages;

import groovy.lang.Binding;

import java.util.Map;

public class GroovyPageBinding extends Binding {
	public GroovyPageBinding() {
		super();
	}

	public GroovyPageBinding(Map variables) {
		super(variables);
	}

	public GroovyPageBinding(String[] args) {
		super(args);
	}

	@Override
	public Object getProperty(String property) {
        if(property.equals("variables")) {
            return super.getVariables();
        }
		return getVariable(property);
	}

	@Override
	public void setProperty(String property, Object newValue) {
		setVariable(property, newValue);
	}
}