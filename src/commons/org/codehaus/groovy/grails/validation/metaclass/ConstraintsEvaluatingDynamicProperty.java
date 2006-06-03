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
package org.codehaus.groovy.grails.validation.metaclass;

import java.io.InputStream;
import java.util.Collections;

import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicProperty;
import org.codehaus.groovy.grails.validation.ConstrainedPropertyBuilder;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
/**
 * This is a dynamic property that instead of returning the closure sets a new proxy meta class for the scope 
 * of the call and invokes the closure itself which builds up a list of ConstrainedProperty instances
 * 
 * @author Graeme Rocher
 * @since 07-Nov-2005
 */
public class ConstraintsEvaluatingDynamicProperty extends AbstractDynamicProperty {
	
	private static final String CONSTRAINTS_GROOVY = "Constraints.groovy";

	private static final Log LOG = LogFactory.getLog(ConstraintsDynamicProperty.class);
	
	public static final String PROPERTY_NAME = "constraints";
	
	public ConstraintsEvaluatingDynamicProperty() {
		super(PROPERTY_NAME);
	}

	public Object get(Object object)  {
		BeanWrapper bean = new BeanWrapperImpl(object);
		try {
			Closure c = (Closure)bean.getPropertyValue(PROPERTY_NAME);
			ConstrainedPropertyBuilder delegate = new ConstrainedPropertyBuilder(object);
			c.setDelegate(delegate);
			c.call();
			return delegate.getConstrainedProperties();			
		}
		// thrown if property doesn't exist
		catch(BeansException be) {
			String className = object.getClass().getName();
			String constraintsScript = className.replaceAll("\\.","/") + CONSTRAINTS_GROOVY;
			InputStream stream = getClass().getClassLoader().getResourceAsStream(constraintsScript);
			
			if(stream!=null) {
				GroovyClassLoader gcl = new GroovyClassLoader();
				try {
					Class scriptClass = gcl.parseClass(stream);
					Script script = (Script)scriptClass.newInstance();
					script.run();
					Closure c = (Closure)script.getBinding().getVariable(PROPERTY_NAME);
					ConstrainedPropertyBuilder delegate = new ConstrainedPropertyBuilder(object);
					c.setDelegate(delegate);
					c.call();
					return delegate.getConstrainedProperties();					
					
				}
				catch (MissingPropertyException mpe) {
					LOG.warn("Unable to evaluate constraints from ["+constraintsScript+"], constraints closure not found!",mpe);
					return Collections.EMPTY_MAP;
				}
				catch (CompilationFailedException e) {
					LOG.error("Compilation error evaluating constraints for class ["+object.getClass()+"]: " + e.getMessage(),e );
					return Collections.EMPTY_MAP;
				} catch (InstantiationException e) {
					LOG.error("Instantiation error evaluating constraints for class ["+object.getClass()+"]: " + e.getMessage(),e );
					return Collections.EMPTY_MAP;
				} catch (IllegalAccessException e) {
					LOG.error("Illegal access error evaluating constraints for class ["+object.getClass()+"]: " + e.getMessage(),e );
					return Collections.EMPTY_MAP;
				}	
			}
			else {
				return Collections.EMPTY_MAP;				
			}
			
		}
	}

	public void set(Object object, Object newValue) {
		throw new UnsupportedOperationException("Cannot set read-only property ["+PROPERTY_NAME+"]");
	}

}
