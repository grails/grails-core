package org.codehaus.groovy.grails.web.pages;

import groovy.lang.GroovyObject;

public interface GroovyPagesUriService {

	public static final String BEAN_ID = "groovyPagesUriService";

	public String getTemplateURI(String controllerName, String templateName);

	public String getDeployedViewURI(String controllerName, String viewName);

	public String getNoSuffixViewURI(GroovyObject controller, String viewName);

	public String getNoSuffixViewURI(String controllerName, String viewName);

	public String getTemplateURI(GroovyObject controller, String templateName);

	public void clear();

}