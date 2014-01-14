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
package org.codehaus.groovy.grails.web.servlet;

import groovy.lang.GroovyObject;

import java.io.Writer;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsTagLibClass;
import org.codehaus.groovy.grails.commons.DefaultGrailsControllerClass;
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods;
import org.codehaus.groovy.grails.web.pages.DefaultGroovyPagesUriService;
import org.codehaus.groovy.grails.web.pages.GroovyPage;
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine;
import org.codehaus.groovy.grails.web.pages.GroovyPagesUriService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.web.util.UrlPathHelper;

/**
 * Holds knowledge about how to obtain certain attributes from either the ServletContext
 * or the HttpServletRequest instance.
 *
 * @see org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
 *
 * @author Graeme Rocher
 * @since 0.3
 */
public class DefaultGrailsApplicationAttributes implements GrailsApplicationAttributes {

    private static Log LOG = LogFactory.getLog(DefaultGrailsApplicationAttributes.class);

    private UrlPathHelper urlHelper = new UrlPathHelper();

    private ServletContext context;
    private ApplicationContext appContext;

    // Beans used very often
    private GroovyPagesTemplateEngine pagesTemplateEngine;
    private GrailsApplication grailsApplication;
    private GroovyPagesUriService groovyPagesUriService;
    private MessageSource messageSource;
    private GrailsPluginManager pluginManager;

    public DefaultGrailsApplicationAttributes(ServletContext context) {
        this.context = context;
        if (context != null) {
            appContext = (ApplicationContext)context.getAttribute(APPLICATION_CONTEXT);
        }
        initBeans();
    }

    public ApplicationContext getApplicationContext() {
        return appContext;
    }

    private void initBeans() {
        if (appContext != null) {
            pagesTemplateEngine = fetchBeanFromAppCtx(GroovyPagesTemplateEngine.BEAN_ID);
            pluginManager = fetchBeanFromAppCtx(GrailsPluginManager.BEAN_NAME);
            grailsApplication = fetchBeanFromAppCtx(GrailsApplication.APPLICATION_ID);
            groovyPagesUriService = fetchBeanFromAppCtx(GroovyPagesUriService.BEAN_ID);
            messageSource = fetchBeanFromAppCtx("messageSource");
        }
        else {
            LOG.warn("ApplicationContext not found in " + APPLICATION_CONTEXT + " attribute of servlet context.");
        }
        if (groovyPagesUriService == null) {
            groovyPagesUriService = new DefaultGroovyPagesUriService();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T fetchBeanFromAppCtx(String name) {
        try {
            return (T)appContext.getBean(name);
        }
        catch(BeansException e) {
            LOG.warn("Bean named '" + name + "' is missing.");
            return null;
        }
    }

    public String getPluginContextPath(HttpServletRequest request) {
        GroovyObject controller = getController(request);
        if (controller != null) {
            String path = pluginManager.getPluginPathForInstance(controller);
            return path == null ? "" : path;
        }

        return "";
    }

    public GroovyObject getController(ServletRequest request) {
        return (GroovyObject)request.getAttribute(CONTROLLER);
    }

    public String getControllerUri(ServletRequest request) {
        return "/" + getControllerName(request);
    }

    private String getControllerName(ServletRequest request) {
        String controllerName = (String) request.getAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE);
        if (controllerName == null || controllerName.length() == 0) {
            GroovyObject controller = getController(request);
            if (controller != null) {
                controllerName = (String)controller.getProperty(ControllerDynamicMethods.CONTROLLER_NAME_PROPERTY);
                request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, controllerName);
                if(((DefaultGrailsControllerClass)controller).getNamespace() != null) {
                    request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAMESPACE_ATTRIBUTE, ((DefaultGrailsControllerClass)controller).getNamespace());
                }
            }
        }
        return controllerName != null ? controllerName : "";
    }

    /**
     * @deprecated Use {@link org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest#getContextPath() instead}
     * @param request The Servlet Reqest
     * @return The Application URI
     */
    @Deprecated
    public String getApplicationUri(ServletRequest request) {
        String appUri = (String) request.getAttribute(GrailsApplicationAttributes.APP_URI_ATTRIBUTE);
        if (appUri == null) {
            appUri = urlHelper.getContextPath((HttpServletRequest)request);
        }
        return appUri;
    }

    public ServletContext getServletContext() {
        return context;
    }

    public FlashScope getFlashScope(ServletRequest request) {
        if (!(request instanceof HttpServletRequest)) {
            return null;
        }

        HttpServletRequest servletRequest = (HttpServletRequest) request;
        HttpSession session = servletRequest.getSession(false);
        FlashScope fs;
        if (session != null) {
            fs = (FlashScope)session.getAttribute(FLASH_SCOPE);
        }
        else {
            fs = (FlashScope)request.getAttribute(FLASH_SCOPE);
        }
        if (fs == null) {
            fs = new GrailsFlashScope();
            if (session!=null) {
                session.setAttribute(FLASH_SCOPE,fs);
            }
            else {
                request.setAttribute(FLASH_SCOPE,fs);
            }
        }
        return fs;
    }

    public String getTemplateUri(CharSequence templateName, ServletRequest request) {
        Assert.notNull(templateName, "Argument [template] cannot be null");
        return groovyPagesUriService.getTemplateURI(getControllerName(request), templateName.toString());
    }

    public String getViewUri(String viewName, HttpServletRequest request) {
        Assert.notNull(viewName, "Argument [view] cannot be null");
        return groovyPagesUriService.getDeployedViewURI(getControllerName(request), viewName);
    }

    public String getControllerActionUri(ServletRequest request) {
        GroovyObject controller = getController(request);
        return (String)controller.getProperty(ControllerDynamicMethods.ACTION_URI_PROPERTY);
    }

    public Errors getErrors(ServletRequest request) {
        return (Errors)request.getAttribute(ERRORS);
    }

    public GroovyPagesTemplateEngine getPagesTemplateEngine() {
        if (pagesTemplateEngine != null) {
            return pagesTemplateEngine;
        }
        if (LOG.isWarnEnabled()) {
            LOG.warn("No bean named [" + GroovyPagesTemplateEngine.BEAN_ID + "] defined in Spring application context!");
        }
        return null;
    }

    public GrailsApplication getGrailsApplication() {
        return grailsApplication;
    }

    public GroovyObject getTagLibraryForTag(HttpServletRequest request, HttpServletResponse response,String tagName) {
        return getTagLibraryForTag(request, response, tagName, GroovyPage.DEFAULT_NAMESPACE);
    }

    public GroovyObject getTagLibraryForTag(HttpServletRequest request, HttpServletResponse response,String tagName, String namespace) {
        String nonNullNamesapce = namespace == null ? GroovyPage.DEFAULT_NAMESPACE : namespace;
        String fullTagName = nonNullNamesapce + ":" + tagName;

        GrailsTagLibClass tagLibClass = (GrailsTagLibClass) getGrailsApplication().getArtefactForFeature(
                TagLibArtefactHandler.TYPE, fullTagName);
        if (tagLibClass == null) {
            return null;
        }

        return (GroovyObject)getApplicationContext().getBean(tagLibClass.getFullName());
    }

    public Writer getOut(HttpServletRequest request) {
        return (Writer)request.getAttribute(OUT);
    }

    public void setOut(HttpServletRequest request, Writer out2) {
        request.setAttribute(OUT, out2);
    }

    public String getNoSuffixViewURI(GroovyObject controller, String viewName) {
        return groovyPagesUriService.getNoSuffixViewURI(controller, viewName);
    }

    public String getTemplateURI(GroovyObject controller, String templateName) {
        return groovyPagesUriService.getTemplateURI(controller, templateName);
    }

    public GroovyPagesUriService getGroovyPagesUriService() {
        return groovyPagesUriService;
    }

    public MessageSource getMessageSource() {
        return messageSource;
    }
}
