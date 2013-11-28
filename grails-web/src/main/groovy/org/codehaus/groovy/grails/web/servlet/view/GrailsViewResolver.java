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
package org.codehaus.groovy.grails.web.servlet.view;

import grails.util.CacheEntry;
import grails.util.GrailsUtil;
import groovy.lang.GroovyObject;

import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerAware;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine;
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator;
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageScriptSource;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.WebUtils;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

/**
 * Evaluates the existance of a view for different extensions choosing which one to delegate to.
 *
 * @author Graeme Rocher
 * @since 0.1
 */
public class GrailsViewResolver extends InternalResourceViewResolver implements PluginManagerAware, GrailsApplicationAware {
    private static final Log LOG = LogFactory.getLog(GrailsViewResolver.class);

    public static final String GSP_SUFFIX = ".gsp";
    public static final String JSP_SUFFIX = ".jsp";

    protected GroovyPagesTemplateEngine templateEngine;
    protected GrailsConventionGroovyPageLocator groovyPageLocator;

    // no need for static cache since GrailsViewResolver is in app context
    private ConcurrentMap<String, CacheEntry<View>> viewCache = new ConcurrentHashMap<String, CacheEntry<View>>();
    private GrailsApplication grailsApplication;
    private boolean developmentMode = GrailsUtil.isDevelopmentEnv();
    private long cacheTimeout=-1;

    /**
     * Constructor.
     */
    public GrailsViewResolver() {
        setCache(false);
    }

    public void setGroovyPageLocator(GrailsConventionGroovyPageLocator groovyPageLocator) {
        this.groovyPageLocator = groovyPageLocator;
    }
    
    @Override
    public View resolveViewName(String viewName, Locale locale) throws Exception {
        return super.resolveViewName(WebUtils.addViewPrefix(viewName), locale);
    }

    @Override
    protected View loadView(String viewName, Locale locale) throws Exception {
        Assert.notNull(templateEngine, "Property [templateEngine] cannot be null");
        if (viewName.endsWith(GSP_SUFFIX)) {
            viewName = viewName.substring(0, viewName.length() - GSP_SUFFIX.length());
        }

        if (developmentMode) {
            return createGrailsView(viewName);
        }

        String viewCacheKey = groovyPageLocator.resolveViewFormat(viewName);

        final String lookupViewName = viewName;
        Callable<View> updater=new Callable<View>() {
            public View call() throws Exception {
                return createGrailsView(lookupViewName);
            }
        };
        try {
            return CacheEntry.getValue(viewCache, viewCacheKey, cacheTimeout, updater);
        } catch (CacheEntry.UpdateException e) {
            e.rethrow();
            // make compiler happy
            return null;            
        }
    }

    protected View createGrailsView(String viewName) throws Exception {
        // try GSP if res is null

        GrailsWebRequest webRequest = WebUtils.retrieveGrailsWebRequest();

        HttpServletRequest request = webRequest.getCurrentRequest();
        GroovyObject controller = webRequest.getAttributes().getController(request);

        if (grailsApplication == null) {
            grailsApplication = getApplicationContext().getBean(GrailsApplication.APPLICATION_ID, GrailsApplication.class);
        }

        GroovyPageScriptSource scriptSource;
        if (controller == null) {
            scriptSource = groovyPageLocator.findViewByPath(viewName);
        }
        else {
            scriptSource = groovyPageLocator.findView(controller, viewName);
        }
        if (scriptSource != null) {
            return createGroovyPageView(webRequest, scriptSource.getURI(), scriptSource);
        }

        return createFallbackView(viewName);
    }

    private View createGroovyPageView(GrailsWebRequest webRequest, String gspView, ScriptSource scriptSource) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Resolved GSP view at URI [" + gspView + "]");
        }
        GroovyPageView gspSpringView = new GroovyPageView();
        gspSpringView.setServletContext(webRequest.getServletContext());
        gspSpringView.setUrl(gspView);
        gspSpringView.setApplicationContext(getApplicationContext());
        gspSpringView.setTemplateEngine(templateEngine);
        gspSpringView.setScriptSource(scriptSource);
        try {
            gspSpringView.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("Error initializing GroovyPageView", e);
        }
        return gspSpringView;
    }

    protected View createFallbackView(String viewName) throws Exception {
        return createJstlView(viewName);
    }
    
    protected View createJstlView(String viewName) throws Exception {
        AbstractUrlBasedView view = buildView(viewName);
        view.setApplicationContext(getApplicationContext());
        view.afterPropertiesSet();
        return view;
    }

    public void setPluginManager(GrailsPluginManager pluginManager) {
        // ignored, here for compatibility
    }

    public void setTemplateEngine(GroovyPagesTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    public long getCacheTimeout() {
        return cacheTimeout;
    }

    public void setCacheTimeout(long cacheTimeout) {
        this.cacheTimeout = cacheTimeout;
    }
}
