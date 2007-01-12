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
package org.codehaus.groovy.grails.web.metaclass;

import groovy.lang.Closure;
import groovy.lang.DelegatingMetaClass;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
/**
 * <p>Special meta class for tag libraries that allows tag libraries to call
 * tags within other libraries without the need for inheritance
 *
 * @author Graeme Rocher
 * @since Apr 3, 20056
 */
public class TagLibMetaClass extends DelegatingMetaClass {
	private MetaClass adaptee;



	public TagLibMetaClass(MetaClass adaptee) {
		super(adaptee);
		this.adaptee = adaptee;
	}

	
	private static final Log LOG = LogFactory.getLog(TagLibMetaClass.class);
	
	
	/**
	 * @return the adaptee
	 */
	public MetaClass getAdaptee() {
		return adaptee;
	}

	/* (non-Javadoc)
	 * @see groovy.lang.ProxyMetaClass#invokeMethod(java.lang.Object, java.lang.String, java.lang.Object[])
	 */
	public Object invokeMethod(Object object, String methodName, Object[] arguments) {
		try {			
			return super.invokeMethod(object, methodName, arguments);
		}
		catch(MissingMethodException mme) {
			GroovyObject taglib = (GroovyObject)object;
			GrailsApplicationAttributes applicationAttributes = (GrailsApplicationAttributes)taglib.getProperty(ControllerDynamicMethods.GRAILS_ATTRIBUTES);
			HttpServletRequest request = (HttpServletRequest)taglib.getProperty(ControllerDynamicMethods.REQUEST_PROPERTY);
			HttpServletResponse response = (HttpServletResponse)taglib.getProperty(ControllerDynamicMethods.RESPONSE_PROPERTY);
			GroovyObject tagLibrary = applicationAttributes.getTagLibraryForTag(request,response,methodName);
			if(tagLibrary == null) throw mme;
			if(tagLibrary.getClass().equals(object.getClass())) throw mme;
			
			if(LOG.isDebugEnabled())
				LOG.debug("Tag ["+methodName+"] not found in existing library, found in ["+tagLibrary.getClass().getName()+"]. Invoking..");
			
			Closure original = (Closure)tagLibrary.getProperty(methodName);
			Closure tag = (Closure)original.clone();
			
			tagLibrary.setProperty(TagLibDynamicMethods.OUT_PROPERTY,taglib.getProperty(TagLibDynamicMethods.OUT_PROPERTY));
			return tag.call(arguments);
		}
	}

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.commons.metaclass.PropertyAccessProxyMetaClass#getProperty(java.lang.Object, java.lang.String)
	 */
	public Object getProperty(Object object, String property) {
		
		try {
			return super.getProperty(object, property);
		}
		catch(MissingPropertyException mpe){
			GroovyObject taglib = (GroovyObject)object;
			GrailsApplicationAttributes applicationAttributes = (GrailsApplicationAttributes)taglib.getProperty(ControllerDynamicMethods.GRAILS_ATTRIBUTES);
			HttpServletRequest request = (HttpServletRequest)taglib.getProperty(ControllerDynamicMethods.REQUEST_PROPERTY);
			HttpServletResponse response = (HttpServletResponse)taglib.getProperty(ControllerDynamicMethods.RESPONSE_PROPERTY);
			GroovyObject tagLibrary = applicationAttributes.getTagLibraryForTag(request,response,property);
			if(tagLibrary == null) throw mpe;
			
			if(LOG.isDebugEnabled())
				LOG.debug("Tag ["+property+"] not found in existing library, found in ["+tagLibrary.getClass().getName()+"]. Retrieving");
			Closure original = (Closure)tagLibrary.getProperty(property);
			return original.clone();
				
		}			
	}	
	
	
}
