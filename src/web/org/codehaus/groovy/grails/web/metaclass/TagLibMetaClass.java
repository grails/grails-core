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
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.pages.GroovyPage;
import org.springframework.beans.BeanWrapperImpl;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Collections;
import java.io.Writer;

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
		if (hasClosure(object, methodName) && (arguments.length == 1 || arguments.length == 2 )) {
            GrailsWebRequest webRequest = getWebRequestFromObject(object);
            return captureTagOutputForLibrary((GroovyObject)object, methodName, arguments, webRequest);
		} else if(hasMethod(object, methodName, arguments)) {
            return super.invokeMethod(object, methodName, arguments);
        }
        else {
            return captureTagOutput(object, methodName, arguments);
        }
	}

    private Object captureTagOutput(Object object, String methodName, Object[] arguments) {
        GrailsWebRequest webRequest = getWebRequestFromObject(object);

        GroovyObject tagLibrary = lookupTagLibrary(methodName,webRequest);

        if(tagLibrary == null) throw new MissingMethodException(methodName, object.getClass(), arguments);
        if(tagLibrary.getClass().equals(object.getClass())) throw new MissingMethodException(methodName, object.getClass(), arguments);

        return captureTagOutputForLibrary(tagLibrary,methodName, arguments, webRequest);
    }

    private Object captureTagOutputForLibrary(GroovyObject tagLibrary, String methodName, Object[] arguments, GrailsWebRequest webRequest) {
        if(LOG.isDebugEnabled())
				LOG.debug("Tag ["+methodName+"] not found in existing library, found in ["+tagLibrary.getClass().getName()+"]. Invoking..");

        Map attrs = Collections.EMPTY_MAP;
        Object body = null;
        if(arguments.length > 0 && arguments[0] instanceof Map)
        attrs = (Map)arguments[0];
        if(arguments.length > 1) {
            body = arguments[1];
        }

        Writer originalOut = webRequest.getOut();
        try {
        return GroovyPage.captureTagOutput(tagLibrary, methodName,attrs,body,webRequest, new BeanWrapperImpl(tagLibrary));
    } finally {
        webRequest.setOut(originalOut);
    }
    }

    private GroovyObject lookupTagLibrary(String methodName, GrailsWebRequest webRequest) {
        return webRequest
                    .getAttributes()
                    .getTagLibraryForTag(webRequest.getCurrentRequest(),webRequest.getCurrentResponse(),methodName);
    }

    /* (non-Javadoc)
      * @see org.codehaus.groovy.grails.commons.metaclass.PropertyAccessProxyMetaClass#getProperty(java.lang.Object, java.lang.String)
      */
	public Object getProperty(Object object, String property) {
        if (hasProperty(object, property)) {
			return super.getProperty(object, property);
		} else {

            GrailsWebRequest webRequest = getWebRequestFromObject(object);

            GroovyObject tagLibrary = lookupTagLibrary(property, webRequest);

            if(tagLibrary == null) throw new MissingPropertyException(property, object.getClass());

			if(LOG.isDebugEnabled())
				LOG.debug("Tag ["+property+"] not found in existing library, found in ["+tagLibrary.getClass().getName()+"]. Retrieving");
			Closure original = (Closure)tagLibrary.getProperty(property);
			return original.clone();

		}
	}

    private GrailsWebRequest getWebRequestFromObject(Object object) {
        GroovyObject taglib = (GroovyObject)object;
        HttpServletRequest request = (HttpServletRequest)taglib.getProperty(ControllerDynamicMethods.REQUEST_PROPERTY);
        return (GrailsWebRequest)request.getAttribute(GrailsApplicationAttributes.WEB_REQUEST);
    }


}
