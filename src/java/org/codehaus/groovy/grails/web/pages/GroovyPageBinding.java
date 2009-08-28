/**
 * 
 */
package org.codehaus.groovy.grails.web.pages;

import groovy.lang.Binding;

import java.io.Writer;
import java.util.Map;

import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

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
}