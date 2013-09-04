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

import groovy.text.Template;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.grails.plugins.BinaryGrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerAware;
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageCompiledScriptSource;
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageScriptSource;
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.sitemesh.GrailsLayoutDecoratorMapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.FrameworkServlet;
import org.springframework.web.util.WebUtils;

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

    private static final String WEB_INF = "/WEB-INF";
    private static final String GRAILS_APP = "/grails-app";

    private ServletContext context;
    private GrailsApplicationAttributes grailsAttributes;

    public GroovyPagesServlet() {
        // use the root web application context always
        setContextAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
    }

    @Override
    protected ServletRequestAttributes buildRequestAttributes(HttpServletRequest request, HttpServletResponse response, RequestAttributes previousAttributes) {
        if(previousAttributes instanceof GrailsWebRequest) {
            return null;
        }
        else {
            return super.buildRequestAttributes(request, response, previousAttributes);
        }
    }

    /**
     * The size of the buffer used when formulating the response
     */
    public static final String SERVLET_INSTANCE = "org.codehaus.groovy.grails.GSP_SERVLET";
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

        boolean isNotInclude = !WebUtils.isIncludeRequest(request) ;
        if (isNotInclude && isSecurePath(pageName)) {
            sendNotFound(response, pageName);
        }
        else {

            GroovyPageScriptSource scriptSource = groovyPagesTemplateEngine.findScriptSource(pageName);

            if (scriptSource == null) {
                scriptSource = findPageInBinaryPlugins(pageName);
            }

            if (scriptSource == null || (isNotInclude && !scriptSource.isPublic())) {
                sendNotFound(response, pageName);
                return;
            }

            renderPageWithEngine(groovyPagesTemplateEngine, request, response, scriptSource);
        }
    }

    public GroovyPagesTemplateEngine getGroovyPagesTemplateEngine() {
        return groovyPagesTemplateEngine;
    }

    protected boolean isSecurePath(String pageName) {
        return pageName.startsWith(WEB_INF) || pageName.startsWith(GRAILS_APP);
    }

    protected void sendNotFound(HttpServletResponse response, String pageName) throws IOException {
        context.log("GroovyPagesServlet:  \"" + pageName + "\" not found");
        response.sendError(404, "\"" + pageName + "\" not found.");
    }

    protected GroovyPageScriptSource findPageInBinaryPlugins(String pageName) {
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
                return new GroovyPageCompiledScriptSource(pageName, pageName, pageClass);
            }
        }
        return null;
    }

    /**
     * Attempts to render the page with the given arguments
     *
     *
     * @param engine The GroovyPagesTemplateEngine to use
     * @param request The HttpServletRequest
     * @param response The HttpServletResponse
     * @param scriptSource The template
     *
     * @throws IOException Thrown when an I/O exception occurs rendering the page
     */
    protected void renderPageWithEngine(GroovyPagesTemplateEngine engine, HttpServletRequest request,
            HttpServletResponse response, GroovyPageScriptSource scriptSource) throws Exception {
        request.setAttribute(GrailsLayoutDecoratorMapper.RENDERING_VIEW, Boolean.TRUE);
        GSPResponseWriter out = createResponseWriter(response);
        try {
            Template template = engine.createTemplate(scriptSource);
            if (template instanceof GroovyPageTemplate) {
                ((GroovyPageTemplate)template).setAllowSettingContentType(true);
            }
            template.make().writeTo(out);
        }
        catch(Exception e) {
            out.setError();
            throw e;
        }
        finally {
            if (out != null) out.close();
        }
    }

    /**
     * Creates a response writer for the given response object
     *
     * @param response The HttpServletResponse
     * @return The created java.io.Writer
     */
    protected GSPResponseWriter createResponseWriter(HttpServletResponse response) {
        GSPResponseWriter out = GSPResponseWriter.getInstance(response);
        GrailsWebRequest webRequest =  (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
        webRequest.setOut(out);
        return out;
    }

    public void setPluginManager(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }
}
