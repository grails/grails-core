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

import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.context.request.RequestContextHolder;
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine;
import org.codehaus.groovy.grails.web.pages.GSPResonseWriter;
import org.codehaus.groovy.grails.web.pages.GroovyPage;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.errors.GrailsWrappedRuntimeException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.util.Map;
import java.util.HashMap;
import java.io.Writer;
import java.io.IOException;

import groovy.text.Template;
import groovy.lang.Writable;

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
public class GroovyPageView extends AbstractUrlBasedView {

    private static final Log LOG = LogFactory.getLog(GroovyPageView.class);
    /**
     * The size of the buffer to use for the GSPReponseWriter
     */
    private static final int BUFFER_SIZE = 8024;
    private GroovyPagesTemplateEngine templateEngine;
    private static final String ERRORS_VIEW = GrailsApplicationAttributes.PATH_TO_VIEWS+"/error"+ GroovyPage.EXTENSION;
    public static final String EXCEPTION_MODEL_KEY = "exception";



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
        super.exposeModelAsRequestAttributes(model, request);

        if(templateEngine == null) throw new IllegalStateException("Property [templateEngine] must be set!");

        renderWithTemplateEngine(templateEngine,model, response);
    }

    /**
     * Renders a page with the specified TemplateEngine, mode and response
     *
     * @param templateEngine The TemplateEngine to use
     * @param model The model to use
     * @param response The HttpServletResponse instance
     * 
     * @throws IOException Thrown when an error occurs writing the response
     */
    protected void renderWithTemplateEngine(GroovyPagesTemplateEngine templateEngine, Map model, HttpServletResponse response) throws IOException {
        Template t = templateEngine.createTemplate(getUrl());
        Writable w = t.make(model);

        Writer out = null;
        try {
            out = createResponseWriter(response);
            w.writeTo(out);
        }
        catch(Exception e) {
            // create fresh response writer
            out = createResponseWriter(response);
            handleException(e, out, templateEngine);
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
    protected void handleException(Exception exception,Writer out, GroovyPagesTemplateEngine engine)  {

        LOG.error("Error processing GSP: " + exception.getMessage(), exception);

        try {
            Template t = engine.createTemplate(ERRORS_VIEW);

            Map model = new HashMap();
            model.put(EXCEPTION_MODEL_KEY,new GrailsWrappedRuntimeException(getServletContext(),exception));
            Writable w = t.make(model);

            w.writeTo(out);
        } catch (Throwable t) {
            LOG.error("Error attempting to render errors view : " + t.getMessage(), t);
            LOG.error("Original exception : " + exception.getMessage(), exception);
        }
    }


    /**
     * Sets the GroovyPageTemplateEngine to use during rendering
     *
     * @param templateEngine The GroovyPagesTemplateEngine instance
     */
    public void setTemplateEngine(GroovyPagesTemplateEngine templateEngine) {
        if(templateEngine == null) throw new IllegalArgumentException("Argument [templateEngine] cannot be null");
        this.templateEngine = templateEngine;
    }

    /**
     * Creates the Response Writer for the specified HttpServletResponse instance
     *
     * @param response The HttpServletResponse instance
     * @return A response Writer
     */
    protected Writer createResponseWriter(HttpServletResponse response) {
        Writer out = GSPResonseWriter.getInstance(response, BUFFER_SIZE);
        GrailsWebRequest webRequest =  (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
        webRequest.setOut(out);
        return out;
    }
}
