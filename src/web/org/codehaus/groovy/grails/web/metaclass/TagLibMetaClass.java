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

import groovy.lang.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.metaclass.AdapterMetaClass;
import org.codehaus.groovy.grails.commons.metaclass.CachingMetaClass;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
/**
 * <p>Special meta class for tag libraries that allows tag libraries to call
 * tags within other libraries without the need for inheritance
 *
 * @author Graeme Rocher
 * @since Apr 3, 20056
 */
public class TagLibMetaClass extends CachingMetaClass implements AdapterMetaClass {
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

    /**
     * Override this to only return true if we have the closure on THIS taglib, else getProperty resolves
     * to other taglibs and things get nasty
     * @param object
     * @param name
     * @return
     */
    protected boolean hasClosure(Object object, String name)
    {
        if(LOG.isDebugEnabled())
            LOG.debug("Closure ["+name+"] being resolved against ["+object.getClass().getName()+"]");

        boolean found = false;
        try
        {
            Object value = super.getProperty(object, name);
            found = value instanceof Closure;
        }
        catch (MissingPropertyException e)
        {
        }
        return found;
    }

    /* (non-Javadoc)
	 * @see groovy.lang.ProxyMetaClass#invokeMethod(java.lang.Object, java.lang.String, java.lang.Object[])
	 */
	public Object invokeMethod(Object object, String methodName, Object[] arguments) {
		if (hasMethod(object, methodName, arguments)) {
			return super.invokeMethod(object, methodName, arguments);
		} else {
			GroovyObject taglib = (GroovyObject)object;
            GroovyObject tagLibrary = lookupTagLibrary(methodName);

            if(tagLibrary == null) throw new MissingMethodException(methodName, object.getClass(), arguments);
			if(tagLibrary.getClass().equals(object.getClass())) throw new MissingMethodException(methodName, object.getClass(), arguments);

			if(LOG.isDebugEnabled())
				LOG.debug("Tag ["+methodName+"] not found in existing library, found in ["+tagLibrary.getClass().getName()+"]. Invoking..");

			Closure original = (Closure)tagLibrary.getProperty(methodName);
			Closure tag = (Closure)original.clone();

			tagLibrary.setProperty(TagLibDynamicMethods.OUT_PROPERTY,taglib.getProperty(TagLibDynamicMethods.OUT_PROPERTY));
			return tag.call(arguments);
		}
	}

    private GroovyObject lookupTagLibrary(String methodName) {
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();

        HttpServletRequest request = webRequest.getCurrentRequest();
        HttpServletResponse response = webRequest.getCurrentResponse();
        return webRequest
                    .getAttributes()
                    .getTagLibraryForTag(request,response,methodName);
    }

    /* (non-Javadoc)
      * @see org.codehaus.groovy.grails.commons.metaclass.PropertyAccessProxyMetaClass#getProperty(java.lang.Object, java.lang.String)
      */
	public Object getProperty(Object object, String property) {
        if (hasProperty(object, property)) {
			return super.getProperty(object, property);
		} else {
			
            GroovyObject tagLibrary = lookupTagLibrary(property);

            if(tagLibrary == null) throw new MissingPropertyException(property, object.getClass());

			if(LOG.isDebugEnabled())
				LOG.debug("Tag ["+property+"] not found in existing library, found in ["+tagLibrary.getClass().getName()+"]. Retrieving");
			Closure original = (Closure)tagLibrary.getProperty(property);
			return original.clone();

		}
	}


}
