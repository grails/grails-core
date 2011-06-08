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

import grails.util.GrailsUtil;
import groovy.lang.Writable;
import groovy.text.Template;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.plugins.BinaryGrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerAware;
import org.codehaus.groovy.grails.web.errors.GrailsWrappedRuntimeException;
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.FrameworkServlet;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NOTE: Based on work done by on the GSP standalone project (https://gsp.dev.java.net/)
 *
 * Main servlet class.  Example usage in web.xml:
 *     <servlet>
 *       <servlet-name>GroovyPagesServlet</servlet-name>
 *       <servlet-class>org.codehaus.groovy.grails.web.pages.GroovyPagesServlet</servlet-class>
 *        <init-param>
 *            <param-name>showSource</param-name>
 *            <param-value>1</param-value>
 *            <description>
 *             Allows developers to view the intermediade source code, when they pass
 *                a showSource argument in the URL (eg /edit/list?showSource=true.
 *          </description>
 *        </init-param>
 *    </servlet>
 *
 * @author Troy Heninger
 * @author Graeme Rocher
 *
 * Date: Jan 10, 2004
 */
public class GroovyPagesServlet extends FrameworkServlet implements PluginManagerAware {

    private static final long serialVersionUID = -1918149859392123495L;

    private static final Log LOG = LogFactory.getLog(GroovyPagesServlet.class);

    private ServletContext context;
    private GrailsApplicationAttributes grailsAttributes;

    public GroovyPagesServlet() {
        // use the root web application context always
        setContextAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
    }

    /**
     * The size of the buffer used when formulating the response
     */
    private static final String ERRORS_VIEW = GrailsApplicationAttributes.PATH_TO_VIEWS+"/error"+GroovyPage.EXTENSION;
    public static final String EXCEPTION_MODEL_KEY = "exception";
    public static final String SERVLET_INSTANCE = "org.codehaus.groovy.grails.GSP_SERVLET";
    private Collection<HandlerExceptionResolver> exceptionResolvers;
    private GroovyPagesTemplateEngine groovyPagesTemplateEngine;
    private GrailsPluginManager pluginManager;
    @SuppressWarnings("rawtypes")
    private Map<String, Class> binaryPluginViewsMap = new ConcurrentHashMap<String, Class>();

    @Override
    protected void initFrameworkServlet() throws ServletException, BeansException {
        context = getServletContext();
        context.log("GSP servlet initialized");
        context.setAttribute(SERVLET_INSTANCE, this);

        final WebApplicationContext webApplicationContext = getWebApplicationContext();
        exceptionResolvers = webApplicationContext.getBeansOfType(HandlerExceptionResolver.class).values();
        grailsAttributes = new DefaultGrailsApplicationAttributes(context);
        webApplicationContext.getAutowireCapableBeanFactory().autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
        groovyPagesTemplateEngine = webApplicationContext.getBean(GroovyPagesTemplateEngine.BEAN_ID,
                GroovyPagesTemplateEngine.class);

    }

    public void setGroovyPagesTemplateEngine(GroovyPagesTemplateEngine groovyPagesTemplateEngine) {
        this.groovyPagesTemplateEngine = groovyPagesTemplateEngine;
    }

    @Override
    protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request.setAttribute(GrailsApplicationAttributes.REQUEST_SCOPE_ID, grailsAttributes);
        request.setAttribute(GroovyPagesServlet.SERVLET_INSTANCE, this);

        String pageName = (String)request.getAttribute(GrailsApplicationAttributes.GSP_TO_RENDER);
        if (StringUtils.isBlank(pageName)) {
            pageName = groovyPagesTemplateEngine.getCurrentRequestUri(request);
        }

        Template template = groovyPagesTemplateEngine.createTemplateForUri(pageName);

        if (template == null) {
            template = findPageInBinaryPlugins(pageName);
        }

        if (template == null) {
            context.log("GroovyPagesServlet:  \"" + pageName + "\" not found");
            response.sendError(404, "\"" + pageName + "\" not found.");
            return;
        }

        renderPageWithEngine(groovyPagesTemplateEngine, request, response, template);
    }

    protected Template findPageInBinaryPlugins(String pageName) {
        if (pageName != null) {
            Class<?> pageClass = binaryPluginViewsMap.get(pageName);
            if (pageClass == null && pluginManager != null) {
                final GrailsPlugin[] allPlugins = pluginManager.getAllPlugins();
                for (GrailsPlugin plugin : allPlugins) {
                    if (plugin instanceof BinaryGrailsPlugin) {
                        BinaryGrailsPlugin binaryPlugin = (BinaryGrailsPlugin) plugin;

                        pageClass = binaryPlugin.resolveView(pageName);
                        if (pageClass != null) {
                            binaryPluginViewsMap.put(pageName, pageClass);
                            break;
                        }
                    }
                }
            }
            if (pageClass != null) {
                return groovyPagesTemplateEngine.createTemplate(pageClass);
            }
        }
        return null;
    }

    /**
     * Attempts to render the page with the given arguments
     *
     * @param engine The GroovyPagesTemplateEngine to use
     * @param request The HttpServletRequest
     * @param response The HttpServletResponse
     * @param template The template
     *
     * @throws IOException Thrown when an I/O exception occurs rendering the page
     */
    protected void renderPageWithEngine(GroovyPagesTemplateEngine engine, HttpServletRequest request,
            HttpServletResponse response, Template template) throws IOException {
        Writer out = createResponseWriter(response);
        try {
            Writable w = template.make();
            w.writeTo(out);
        }
        catch(Exception e) {
            out = createResponseWriter(response);
            handleException(request, response, e, out,engine);
        }
        finally {
            if (out != null) out.close();
        }
    }

    /**
     * Performs exception handling by attempting to render the Errors view
     *
     * @param request
     * @param response
     * @param exception The exception that occured
     * @param out The Writer
     * @param engine The GSP engine    @throws IOException Thrown when an I/O exception occurs rendering the page
     */
    protected void handleException(HttpServletRequest request, HttpServletResponse response,
            Exception exception, Writer out, GroovyPagesTemplateEngine engine) {

        if (exceptionResolvers == null) {
            defaultExceptionHandling(exception, out, engine);
            return;
        }

        ModelAndView exMv = null;
        for (HandlerExceptionResolver exceptionResolver : exceptionResolvers) {
            exMv = exceptionResolver.resolveException(request, response, this, exception);
            if (exMv != null) break;
        }

        if (exMv != null) {
            try {
                exMv.getView().render(exMv.getModel(),request, response);
            }
            catch (Exception e) {
                defaultExceptionHandling(exception, out, engine);
            }
        }
    }

    private void defaultExceptionHandling(Exception exception, Writer out, GroovyPagesTemplateEngine engine) {
        if (LOG.isErrorEnabled()) {
            LOG.error("Error processing GSP: " + exception.getMessage(), exception);
        }

        try {
            Template t = engine.createTemplate(ERRORS_VIEW);

            Map<String, GrailsWrappedRuntimeException> model = Collections.singletonMap(
                    EXCEPTION_MODEL_KEY, new GrailsWrappedRuntimeException(context, exception));
            t.make(model).writeTo(out);
        }
        catch (Throwable t) {
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
        PrintWriter out = GSPResponseWriter.getInstance(response);
        GrailsWebRequest webRequest =  (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
        webRequest.setOut(out);
        return out;
    }

    public void setPluginManager(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }
}
