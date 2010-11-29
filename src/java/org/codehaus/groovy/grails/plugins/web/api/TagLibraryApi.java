/*
 * Copyright 2010 the original author or authors.
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
package org.codehaus.groovy.grails.plugins.web.api;

import java.io.Writer;

import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.web.pages.GroovyPageBinding;
import org.codehaus.groovy.grails.web.pages.GroovyPageOutputStack;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * API for Tag libraries in a Grails application
 * 
 * @author Graeme Rocher
 * @since 1.4
 *
 */
public class TagLibraryApi extends CommonWebApi {

	
    public TagLibraryApi(GrailsPluginManager pluginManager) {
		super(pluginManager);
		
	}

    /**
     * Throws a GrailsTagException
     * 
     * @param instance
     * @param message
     */
	public void throwTagError (Object instance, String message) { throw new GrailsTagException(message); }

	/**
	 * Obtains the page scope instance
	 * 
	 * @param instance The tag library
	 * @return  The page scope instance
	 */
	public GroovyPageBinding getPageScope(Object instance) {
        RequestAttributes request = RequestContextHolder.currentRequestAttributes();
        GroovyPageBinding binding = (GroovyPageBinding) request.getAttribute(GrailsApplicationAttributes.PAGE_SCOPE, RequestAttributes.SCOPE_REQUEST);
        if (binding == null) {
            binding = new GroovyPageBinding();
            request.setAttribute(GrailsApplicationAttributes.PAGE_SCOPE, binding, RequestAttributes.SCOPE_REQUEST);
        }
        return binding;
    }

	/**
	 * Obtains the currently output writer
	 * @param writer The writer
	 * @return
	 */
    public Writer getOut(Object instance) {
        return GroovyPageOutputStack.currentWriter();
    }
    
    /**
     * Sets the current output writer
     * @param newOut The new output writer
     */
    public void setOut(Object instance, Writer newOut) {
        GroovyPageOutputStack.currentStack().push(newOut,true);
    }
}
