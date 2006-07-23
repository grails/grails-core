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

import groovy.lang.GroovyObject;
import groovy.lang.MissingMethodException;

import java.beans.IntrospectionException;
import java.io.Writer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethodInvocation;
import org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethodsInterceptor;
import org.codehaus.groovy.grails.commons.metaclass.GenericDynamicProperty;
import org.codehaus.groovy.grails.commons.metaclass.ProxyMetaClass;
import org.codehaus.groovy.grails.web.servlet.FlashScope;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.GrailsHttpServletRequest;
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException;

/**
 * <p>Represents a controller class in Grails.
 *
 * @author Graeme Rocher
 * @since Jan 14, 20056
 */
public class TagLibDynamicMethods extends AbstractDynamicMethodsInterceptor {

	
    public static final String OUT_PROPERTY = "out";
    private static final String THROW_TAG_ERROR_METHOD = "throwTagError";

    public TagLibDynamicMethods(GroovyObject taglib, GroovyObject controller) throws IntrospectionException {    	
        
    	ProxyMetaClass pmc = TagLibMetaClass.getTagLibInstance(taglib.getClass());
 		pmc.setInterceptor( this );
 		taglib.setMetaClass(pmc);	
        
        ProxyMetaClass controllerMetaClass = (ProxyMetaClass)controller.getMetaClass();
        ControllerDynamicMethods controllerDynamicMethods = (ControllerDynamicMethods)controllerMetaClass.getInterceptor();
        
        addDynamicProperty(new GenericDynamicProperty(OUT_PROPERTY, Writer.class,false));

        // add dynamic properties (shared with controller)
        addDynamicProperty(controllerDynamicMethods.getDynamicProperty(GetParamsDynamicProperty.PROPERTY_NAME));
        addDynamicProperty(controllerDynamicMethods.getDynamicProperty(GetSessionDynamicProperty.PROPERTY_NAME));
        addDynamicProperty(controllerDynamicMethods.getDynamicProperty(ControllerDynamicMethods.REQUEST_PROPERTY));
        addDynamicProperty(controllerDynamicMethods.getDynamicProperty(ControllerDynamicMethods.RESPONSE_PROPERTY) );
        addDynamicProperty(controllerDynamicMethods.getDynamicProperty(ControllerDynamicMethods.SERVLET_CONTEXT) );
        addDynamicProperty(controllerDynamicMethods.getDynamicProperty(ControllerDynamicMethods.GRAILS_ATTRIBUTES) );
        addDynamicProperty(controllerDynamicMethods.getDynamicProperty(ControllerDynamicMethods.FLASH_SCOPE_PROPERTY) );
        addDynamicProperty(controllerDynamicMethods.getDynamicProperty(ControllerDynamicMethods.GRAILS_APPLICATION));        

        addDynamicMethodInvocation(new AbstractDynamicMethodInvocation(THROW_TAG_ERROR_METHOD) {
            public Object invoke(Object target, Object[] arguments) {
                if(arguments.length == 0)
                    throw new MissingMethodException(THROW_TAG_ERROR_METHOD,target.getClass(),arguments);
                throw new GrailsTagException(arguments[0].toString());
            }
        });
    }

	public TagLibDynamicMethods(GroovyObject taglib, HttpServletRequest request,HttpServletResponse response) throws IntrospectionException {
    	ProxyMetaClass pmc = TagLibMetaClass.getTagLibInstance(taglib.getClass());
 		pmc.setInterceptor( this );
 		taglib.setMetaClass(pmc);	

 		GrailsApplicationAttributes attrs = (GrailsApplicationAttributes)request.getAttribute(GrailsApplicationAttributes.REQUEST_SCOPE_ID);
 		
 		addDynamicProperty(new GenericDynamicProperty(OUT_PROPERTY, Writer.class,false));
        addDynamicProperty(new GetParamsDynamicProperty(request,response));
        addDynamicProperty(new GetSessionDynamicProperty(request,response)); 		
        addDynamicProperty(new GenericDynamicProperty(ControllerDynamicMethods.REQUEST_PROPERTY, HttpServletRequest.class,new GrailsHttpServletRequest( request ),true) );
        addDynamicProperty(new GenericDynamicProperty(ControllerDynamicMethods.RESPONSE_PROPERTY, HttpServletResponse.class,response,true) );
        addDynamicProperty(new GenericDynamicProperty(ControllerDynamicMethods.SERVLET_CONTEXT, ServletContext.class,attrs.getServletContext(),true) );
        addDynamicProperty(new GenericDynamicProperty(ControllerDynamicMethods.FLASH_SCOPE_PROPERTY, FlashScope.class,attrs.getFlashScope(request),false) );
        addDynamicProperty(new GenericDynamicProperty(ControllerDynamicMethods.GRAILS_ATTRIBUTES, GrailsApplicationAttributes.class,attrs,true));
        addDynamicProperty(new GenericDynamicProperty(ControllerDynamicMethods.GRAILS_APPLICATION, GrailsApplication.class,attrs.getGrailsApplication(),true));        
 		
        addDynamicMethodInvocation(new AbstractDynamicMethodInvocation(THROW_TAG_ERROR_METHOD) {
            public Object invoke(Object target, Object[] arguments) {
                if(arguments.length == 0)
                    throw new MissingMethodException(THROW_TAG_ERROR_METHOD,target.getClass(),arguments);
                throw new GrailsTagException(arguments[0].toString());
            }
        });        
	}      
}
