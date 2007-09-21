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
package org.codehaus.groovy.grails.web.pages;

import groovy.lang.Writable;
import groovy.text.Template;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.web.errors.GrailsWrappedRuntimeException;
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.core.io.Resource;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import grails.util.GrailsUtil;

/**
 * NOTE: Based on work done by on the GSP standalone project (https://gsp.dev.java.net/)
 *
 * Main servlet class.  Example usage in web.xml:
 * 	<servlet>
 *       <servlet-name>GroovyPagesServlet</servlet-name>
 *       <servlet-class>org.codehaus.groovy.grails.web.pages.GroovyPagesServlet</servlet-class>
 *		<init-param>
 *			<param-name>showSource</param-name>
 *			<param-value>1</param-value>
 *			<description>
 *             Allows developers to view the intermediade source code, when they pass
 *				a showSource argument in the URL (eg /edit/list?showSource=true.
 *          </description>
 *		</init-param>
 *    </servlet>
 *
 * @author Troy Heninger
 * @author Graeme Rocher
 *
 * Date: Jan 10, 2004
 *
 */
public class GroovyPagesServlet extends HttpServlet  {
	
	private static final Log LOG = LogFactory.getLog(GroovyPagesServlet.class);
	
    private ServletContext context;

    private GrailsApplicationAttributes grailsAttributes;

    /**
     * The size of the buffer used when formulating the response
     */
    private static final int BUFFER_SIZE = 8192;
    private static final String ERRORS_VIEW = GrailsApplicationAttributes.PATH_TO_VIEWS+"/error"+GroovyPage.EXTENSION;
    public static final String EXCEPTION_MODEL_KEY = "exception";


    /**
     * Initialize the servlet, set it's parameters.
     * @param config servlet settings
     */
    public void init(ServletConfig config) {
        // Get the servlet context
        context = config.getServletContext();
        context.log("GSP servlet initialized");

        this.grailsAttributes = new DefaultGrailsApplicationAttributes(context);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPage(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPage(request, response);
    }

    /**
     * @return the servlet context
     */
    public ServletContext getServletContext() { return context; }


    /**
     * Execute page and produce output.
     * @param request The HttpServletRequest   insance
     * @param response The HttpServletResponse instance
     * @throws ServletException Thrown when an exception occurs executing the servlet
     * @throws IOException Thrown when an IOException occurs executing the servlet
     */
    public void doPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	request.setAttribute(GrailsApplicationAttributes.REQUEST_SCOPE_ID, grailsAttributes);

        GroovyPagesTemplateEngine engine = grailsAttributes.getPagesTemplateEngine();
        String pageName = (String)request.getAttribute(GrailsApplicationAttributes.GSP_TO_RENDER);
        if(StringUtils.isBlank(pageName)) {
            pageName = engine.getCurrentRequestUri(request);
        }

        Resource page = engine.getResourceForUri(pageName);
        if (page == null) {
            context.log("GroovyPagesServlet:  \"" + pageName + "\" not found");
            response.sendError(404, "\"" + pageName + "\" not found.");
            return;
        }

        renderPageWithEngine(engine, request, response, page);
    }

    /**
     * Attempts to render the page with the given arguments
     *
     * @param engine The GroovyPagesTemplateEngine to use
     * @param request The HttpServletRequest
     * @param response The HttpServletResponse
     * @param pageResource The URL of the page
     *
     * @throws IOException Thrown when an I/O exception occurs rendering the page
     * @throws ServletException Thrown when an exception occurs in the servlet environment
     */
    protected void renderPageWithEngine(GroovyPagesTemplateEngine engine, HttpServletRequest request, HttpServletResponse response, Resource pageResource) throws IOException, ServletException {
         Writer out = createResponseWriter(response);
        try {
            Template t = engine.createTemplate(pageResource);
            if(t == null) {
                context.log("GroovyPagesServlet:  \"" + pageResource.getDescription() + "\" not found");
                response.sendError(404, "\"" + pageResource.getDescription() + "\" not found.");
                return;
            }
            Writable w = t.make();


            w.writeTo(out);
        }
        catch(Exception e) {
            out = createResponseWriter(response);
            handleException(e, out,engine);
        }
        finally {
            if (out != null) out.close();
        }
    }

    /**
     * Performs exception handling by attempting to render the Errors view
     *
     * @param exception The exception that occured
     * @param out The Writer
     * @param engine The GSP engine

     * @throws IOException Thrown when an I/O exception occurs rendering the page
     * @throws ServletException Thrown when an exception occurs in the servlet environment
     */
    protected void handleException(Exception exception,Writer out, GroovyPagesTemplateEngine engine) throws ServletException, IOException {
        GrailsUtil.deepSanitize(exception);
        if(LOG.isErrorEnabled())
            LOG.error("Error processing GSP: " + exception.getMessage(), exception);

        try {
            Template t = engine.createTemplate(ERRORS_VIEW);

            Map model = new HashMap();
            model.put(EXCEPTION_MODEL_KEY,new GrailsWrappedRuntimeException(context,exception));
            Writable w = t.make(model);

            w.writeTo(out);
        } catch (Throwable t) {
            LOG.error("Error attempting to render errors view : " + t.getMessage(), t);
            LOG.error("Original exception : " + exception.getMessage(), exception);
        }
    }

    /**
     * Creates a response writer for the given response object
     *
     * @param response The HttpServletResponse
     * @return The created java.io.Writer
     */
    protected Writer createResponseWriter(HttpServletResponse response) {
        Writer out = GSPResponseWriter.getInstance(response, BUFFER_SIZE);
        GrailsWebRequest webRequest =  (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
        webRequest.setOut(out);
        return out;
    }

}
