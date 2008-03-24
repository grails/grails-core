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
package org.codehaus.groovy.grails.web.servlet;

import groovy.lang.GroovyObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsTagLibClass;
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler;
import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods;
import org.codehaus.groovy.grails.web.pages.GroovyPage;
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine;
import org.codehaus.groovy.grails.web.pages.exceptions.GroovyPagesException;
import org.codehaus.groovy.grails.plugins.PluginMetaManager;
import org.springframework.context.ApplicationContext;
import org.springframework.validation.Errors;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.Writer;

/**
 * Implementation of the GrailsApplicationAttributes interface that holds knowledge about how to obtain
 * certain attributes from either the ServletContext or the HttpServletRequest instance.
 *
 * @see org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
 *
 * @author Graeme Rocher
 * @since 0.3
 *
 * Created: 17-Jan-2006
 */
public class DefaultGrailsApplicationAttributes implements GrailsApplicationAttributes {
	
	private static Log LOG = LogFactory.getLog(DefaultGrailsApplicationAttributes.class);

    private UrlPathHelper urlHelper = new UrlPathHelper();
    
    private ServletContext context;

    public DefaultGrailsApplicationAttributes(ServletContext context) {
        this.context = context;
    }

    public ApplicationContext getApplicationContext() {
        return (ApplicationContext)this.context.getAttribute(APPLICATION_CONTEXT);
    }

    public String getPluginContextPath(HttpServletRequest request) {
        GroovyObject controller = getController(request);
        GrailsApplication application = getGrailsApplication();

        if(controller != null && application != null) {
            final Class controllerClass = controller.getClass();
            PluginMetaManager metaManager = (PluginMetaManager)getApplicationContext().getBean(PluginMetaManager.BEAN_ID);
            String path = metaManager.getPluginPathForResource(controllerClass.getName());
            return path == null ? "" : path;
        }
        else {
            return "";
        }
    }

    public GroovyObject getController(ServletRequest request) {
        return (GroovyObject)request.getAttribute(CONTROLLER);
    }

    public String getControllerUri(ServletRequest request) {
        GroovyObject controller = getController(request);
        if(controller != null)
            return (String)controller.getProperty(ControllerDynamicMethods.CONTROLLER_URI_PROPERTY);
        else {
            return "/" + request.getAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE);
        }
    }

    public String getApplicationUri(ServletRequest request) {
        return this.urlHelper.getContextPath((HttpServletRequest)request);
    }

    public ServletContext getServletContext() {
        return this.context;
    }

    public FlashScope getFlashScope(ServletRequest request) {
        if(request instanceof HttpServletRequest) {
            HttpServletRequest servletRequest = (HttpServletRequest) request;
            HttpSession session = servletRequest.getSession(false);
            FlashScope fs;
            if(session != null) {
                fs = (FlashScope)session.getAttribute(FLASH_SCOPE);
            }
            else {
                fs = (FlashScope)request.getAttribute(FLASH_SCOPE);
            }
            if(fs == null) {
                fs = new GrailsFlashScope();
                if(session!=null) {
                    session.setAttribute(FLASH_SCOPE,fs);
                }
                else {
                    request.setAttribute(FLASH_SCOPE,fs);
                }
            }
            return fs;
        }
        return null;
    }

    public String getTemplateUri(String templateName, ServletRequest request) {       	
       StringBuffer buf = new StringBuffer();
       
       if(templateName.startsWith("/")) {
    	   String tmp = templateName.substring(1,templateName.length());
    	   if(tmp.indexOf('/') > -1) {
    		   buf.append('/');
               int i = tmp.lastIndexOf('/');
               buf.append(tmp.substring(0, i));
    		   buf.append("/_");
    		   buf.append(tmp.substring(i + 1,tmp.length()));
    	   }
    	   else {
    		   buf.append("/_");
    		   buf.append(templateName.substring(1,templateName.length()));
    	   }
       }
       else {
           String pathToTemplate = "";

           int lastSlash = templateName.lastIndexOf('/');
           if (lastSlash > -1) {
               pathToTemplate = templateName.substring(0, lastSlash + 1);
               templateName = templateName.substring(lastSlash + 1);
           }
           buf.append(getControllerUri(request))
                   .append("/")
                   .append(pathToTemplate)
                   .append("_")
                   .append(templateName);
       }
       return buf
       			.append(".gsp")
       			.toString();
   }

    public String getControllerActionUri(ServletRequest request) {
        GroovyObject controller = getController(request);

        return (String)controller.getProperty(ControllerDynamicMethods.ACTION_URI_PROPERTY);
    }

    public Errors getErrors(ServletRequest request) {
        return (Errors)request.getAttribute(ERRORS);
    }

    public GroovyPagesTemplateEngine getPagesTemplateEngine() {
       ApplicationContext appCtx = getApplicationContext();
       if(appCtx.containsBean(GroovyPagesTemplateEngine.BEAN_ID)) {
            return (GroovyPagesTemplateEngine)appCtx.getBean(GroovyPagesTemplateEngine.BEAN_ID);
       }
       else {
           throw new GroovyPagesException("No bean named ["+GroovyPagesTemplateEngine.BEAN_ID+"] defined in Spring application context!");
       }
    }

    public GrailsApplication getGrailsApplication() {
        return (GrailsApplication)getApplicationContext()
                                    .getBean(GrailsApplication.APPLICATION_ID);
    }

    
    public GroovyObject getTagLibraryForTag(HttpServletRequest request, HttpServletResponse response,String tagName) {
    	return getTagLibraryForTag(request, response, tagName, GroovyPage.DEFAULT_NAMESPACE);
    }
    
	public GroovyObject getTagLibraryForTag(HttpServletRequest request, HttpServletResponse response,String tagName, String namespace) {
		String nonNullNamesapce = namespace == null ? GroovyPage.DEFAULT_NAMESPACE : namespace;
		String fullTagName = nonNullNamesapce + ":" + tagName;
		
		GrailsTagLibClass tagLibClass = (GrailsTagLibClass) getGrailsApplication().getArtefactForFeature(
                TagLibArtefactHandler.TYPE, fullTagName);
		if(tagLibClass == null)return null;
		return (GroovyObject)getApplicationContext()
												.getBean(tagLibClass.getFullName());
	}

	public String getViewUri(String viewName, HttpServletRequest request) {
	       StringBuffer buf = new StringBuffer(PATH_TO_VIEWS);
	       
	       if(viewName.startsWith("/")) {
	    	   String tmp = viewName.substring(1,viewName.length());
	    	   if(tmp.indexOf('/') > -1) {
	    		   buf.append('/');
	    		   buf.append(tmp.substring(0,tmp.lastIndexOf('/')));
	    		   buf.append("/");
	    		   buf.append(tmp.substring(tmp.lastIndexOf('/') + 1,tmp.length()));
	    	   }
	    	   else {
	    		   buf.append("/");
	    		   buf.append(viewName.substring(1,viewName.length()));
	    	   }
	       }
	       else {
	           buf.append(getControllerUri(request))
	           .append("/")
	           .append(viewName);
	    	   
	       }
	       return buf
	       			.append(".gsp")
	       			.toString();
	}


	public Writer getOut(HttpServletRequest request) {
		return (Writer)request.getAttribute(OUT);
	}

	public void setOut(HttpServletRequest request, Writer out2) {
		request.setAttribute(OUT, out2);
	}
}
