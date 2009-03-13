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
package org.codehaus.groovy.grails.compiler.injection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoader;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

/**
 * A Groovy compiler operation that gets plugged into the GroovyClassLoader instance to 
 * add custom properties to classes at compile time
 * 
 * @author Graeme Rocher
 *
 * @since 0.2
 * 
 * Created: 20th June 2006
 */
public class GrailsInjectionOperation extends CompilationUnit.PrimaryClassNodeOperation implements ApplicationContextAware {
	
	private ApplicationContext applicationContext;
	private GrailsResourceLoader resourceLoader;
	private static final Log LOG = LogFactory.getLog(GrailsInjectionOperation.class);
	
	public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
		// should always have an application context
		if(applicationContext != null && resourceLoader != null) {
			try {
				URL url;
				if(GrailsResourceUtils.isGrailsPath(source.getName())) {
					url = resourceLoader.loadGroovySource(GrailsResourceUtils.getClassName(source.getName()));
				}
				else {
					url = resourceLoader.loadGroovySource(source.getName());
				}
				// if the source is a domain class get the domain class injectors
				// and perform injection. Injection allows us to add properties,methods
				// etc. at 'compile' time
				if(GrailsResourceUtils.isDomainClass(url)) {
					Map injectors = applicationContext.getBeansOfType(GrailsDomainClassInjector.class);
					for (Iterator i = injectors.values().iterator(); i.hasNext();) {
						GrailsDomainClassInjector injector = (GrailsDomainClassInjector) i.next();
						
						injector.performInjection(source,context,classNode);
					}
				}
				
			} catch (MalformedURLException e) {
				LOG.error("Error loading URL during addition of compile time properties: " + e.getMessage(),e);
				throw new CompilationFailedException(Phases.CONVERSION,source,e);
			}			
		}
	}
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
	public void setResourceLoader(GrailsResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}
}
