/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.web.servlet.view;

import grails.util.GrailsUtil;
import groovy.lang.Writable;
import groovy.text.Template;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.web.pages.GSPResponseWriter;
import org.codehaus.groovy.grails.web.pages.GroovyPage;
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine;
import org.codehaus.groovy.grails.web.pages.exceptions.GroovyPagesException;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

/**
 * A Spring View that renders Groovy Server Pages to the reponse. It requires an instance
 * of GroovyPagesTemplateEngine to be set and will render to view returned by the getUrl()
 * method of AbstractUrlBasedView
 *
 * This view also requires an instance of GrailsWebRequest to be bound to the currently
 * executing Thread using Spring's RequestContextHolder. This can be done with by adding
 * the GrailsWebRequestFilter.
 *
 * @see #getUrl()
 * @see org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
 * @see org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequestFilter
 * @see org.springframework.web.context.request.RequestContextHolder
 *
 * @author Graeme Rocher
 * @since 0.4
 *
 *        <p/>
 *        Created: Feb 27, 2007
 *        Time: 8:25:10 AM
 */
public class GroovyPageView extends AbstractUrlBasedView  {
    private static final Log LOG = LogFactory.getLog(GroovyPageView.class);
    private static final String ERRORS_VIEW = GrailsApplicationAttributes.PATH_TO_VIEWS+"/error"+ GroovyPage.EXTENSION;
    public static final String EXCEPTION_MODEL_KEY = "exception";
    private GroovyPagesTemplateEngine templateEngine;


	/**
     * Delegates to renderMergedOutputModel(..)
     *
     * @see #renderMergedOutputModel(java.util.Map, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     *
     * @param model The view model
     * @param request The HttpServletRequest
     * @param response The HttpServletResponse
     * @throws Exception When an error occurs rendering the view
     */
    protected final void renderMergedOutputModel(Map model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // templateEngine is always same instance in context, can use cached; removed static cache in GrailsViewResolver
        if(templateEngine == null) throw new IllegalStateException("No GroovyPagesTemplateEngine found in ApplicationContext!");
        
        super.exposeModelAsRequestAttributes(model, request);
        renderWithTemplateEngine(templateEngine,model, response, request); // new ModelExposingHttpRequestWrapper(request, model)
    }

    
    /**
     * Replaces the requirement for "super.exposeModelAsRequestAttributes(model, request);" in renderMergedOutputModel
     * 
     *  
     *  not is use, since causes bugs, could improve performance
     * 
     * @author Lari Hotari
     *
     */
    /*
    private static class ModelExposingHttpRequestWrapper extends HttpServletRequestWrapper {
    	Map model;
    	
		public ModelExposingHttpRequestWrapper(HttpServletRequest request, Map model) {
			super(request);
			this.model=model;
		}

		@Override
		public Object getAttribute(String name) {
			Object value=super.getAttribute(name);
			if(value==null) {
				return model.get(name);
			}
			return value;
		}

		@Override
		public Enumeration getAttributeNames() {
			return CollectionUtils.append(super.getAttributeNames(), CollectionUtils.asEnumeration(model.keySet().iterator()));
		}

		@Override
		public void removeAttribute(String name) {
			super.removeAttribute(name);
			model.remove(name);
		}

		@Override
		public void setAttribute(String name, Object o) {
			super.setAttribute(name, o);
			if(o == null) {
				model.remove(name);
			}
		}
    }
    */
    
    /**
     * Renders a page with the specified TemplateEngine, mode and response
     *
     * @param templateEngine The TemplateEngine to use
     * @param model The model to use
     * @param response The HttpServletResponse instance
     * @param request The HttpServletRequest
     *
     * @throws java.io.IOException Thrown when an error occurs writing the response
     */
    protected void renderWithTemplateEngine(GroovyPagesTemplateEngine templateEngine, Map model,
                                            HttpServletResponse response, HttpServletRequest request) throws IOException {
        Writer out = null;
        try {
            out = createResponseWriter(response);
            Template t = templateEngine.createTemplate(getUrl());
            Writable w = t.make(model);

            w.writeTo(out);
        }
        catch(Exception e) {
            // create fresh response writer
            out = createResponseWriter(response);
            handleException(e, out, templateEngine, request, response);
        }
        finally {
            if(out!=null)out.close();
        }
    }

    /**
     * Performs exception handling by attempting to render the Errors view
     *
     * @param exception The exception that occured
     * @param out The Writer
     * @param engine The GSP engine
     */
    protected void handleException(Exception exception, Writer out, GroovyPagesTemplateEngine engine, HttpServletRequest request, HttpServletResponse response)  {

        GrailsUtil.deepSanitize(exception);
        LOG.error("Error processing GroovyPageView: " + exception.getMessage(), exception);
        if(exception instanceof GroovyPagesException) {
            throw (GroovyPagesException) exception;
        }
        else {
            throw new GroovyPagesException("Error processing GroovyPageView: " + exception.getMessage(), exception,-1, getUrl());
        }
    }


    /**
     * Creates the Response Writer for the specified HttpServletResponse instance
     *
     * @param response The HttpServletResponse instance
     * @return A response Writer
     */
    //TODO this method is dupe'd across GSP servlet, reload servlet and here...
    protected Writer createResponseWriter(HttpServletResponse response) {
        PrintWriter out = GSPResponseWriter.getInstance(response);
        GrailsWebRequest webRequest =  (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
        webRequest.setOut(out);
        return out;
    }

    public void setTemplateEngine(GroovyPagesTemplateEngine templateEngine) {
		this.templateEngine = templateEngine;
	}
}
