/* Copyright 2004-2005 the original author or authors.
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

import groovy.lang.GroovyObject;


/**
 * Provides methods to lookup URIs of views and templates
 *
 * @author Graeme Rocher
 * @since 1.2
 *
 */
public interface GroovyPagesUriService {

	public static final String BEAN_ID = "groovyPagesUriService";

	public String getTemplateURI(String controllerName, String templateName);

	public String getDeployedViewURI(String controllerName, String viewName);

	public String getNoSuffixViewURI(GroovyObject controller, String viewName);

	public String getNoSuffixViewURI(String controllerName, String viewName);

	public String getTemplateURI(GroovyObject controller, String templateName);

	public void clear();

}