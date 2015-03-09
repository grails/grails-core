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
package org.grails.web.servlet.view;

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
import org.grails.gsp.GroovyPagesTemplateEngine;
import org.grails.web.gsp.io.GrailsConventionGroovyPageLocator;
import org.grails.gsp.io.GroovyPageScriptSource;
import org.grails.web.servlet.mvc.GrailsWebRequest;
import org.grails.web.util.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
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
public class GroovyPageViewResolver extends InternalResourceViewResolver implements GrailsViewResolver {
    private static final Log LOG = LogFactory.getLog(GroovyPageViewResolver.class);

    public static final String GSP_SUFFIX = ".gsp";
    public static final String JSP_SUFFIX = ".jsp";

    protected GroovyPagesTemplateEngine templateEngine;
    protected GrailsConventionGroovyPageLocator groovyPageLocator;

    private ConcurrentMap<String, CacheEntry<View>> viewCache = new ConcurrentHashMap<String, CacheEntry<View>>();
    private boolean allowGrailsViewCaching = !GrailsUtil.isDevelopmentEnv();
    private long cacheTimeout=-1;
    private boolean resolveJspView = false;
    
    /**
     * Constructor.
     */
    public GroovyPageViewResolver() {
        setCache(false);
        setOrder(Ordered.LOWEST_PRECEDENCE - 20);
    }
    
    public GroovyPageViewResolver(GroovyPagesTemplateEngine templateEngine,
            GrailsConventionGroovyPageLocator groovyPageLocator) {
        this();
        this.templateEngine = templateEngine;
        this.groovyPageLocator = groovyPageLocator;
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

        if (!allowGrailsViewCaching) {
            return createGrailsView(viewName);
        }

        String viewCacheKey = groovyPageLocator.resolveViewFormat(viewName);
        
        String currentControllerKeyPrefix = resolveCurrentControllerKeyPrefixes();
        if (currentControllerKeyPrefix != null) {
            viewCacheKey = currentControllerKeyPrefix + ':' + viewCacheKey;
        }

        CacheEntry<View> entry = viewCache.get(viewCacheKey);

        final String lookupViewName = viewName;
        Callable<View> updater=new Callable<View>() {
            public View call() throws Exception {
                try {
                    return createGrailsView(lookupViewName);
                }
                catch (Exception e) {
                    throw new WrappedInitializationException(e);
                }
            }
        };

        View view = null;
        if (entry == null) {
            try {
                return CacheEntry.getValue(viewCache, viewCacheKey, cacheTimeout, updater);
            }
            catch (CacheEntry.UpdateException e) {
                e.rethrowCause();
                // make compiler happy
                return null;
            }
        }

        try {
            view = entry.getValue(cacheTimeout, updater, true, null);
        } catch (WrappedInitializationException e) {
            e.rethrowCause();
        }

        return view;
    }

    /**
     * @return prefix for cache key that contains current controller's context (currently plugin and namespace)
     */
    protected String resolveCurrentControllerKeyPrefixes() {
        String pluginContextPath = null;
        String namespace = null;
        GrailsWebRequest webRequest = GrailsWebRequest.lookup();
        if(webRequest != null) {
            namespace = webRequest.getControllerNamespace();
            pluginContextPath = (webRequest.getAttributes() != null && webRequest.getCurrentRequest() != null) ? webRequest.getAttributes().getPluginContextPath(webRequest.getCurrentRequest()) : null;
            return (pluginContextPath != null ? pluginContextPath : "-") + "," + (namespace != null ? namespace : "-");
        } else {
            return null;
        }
    }

    private static class WrappedInitializationException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public WrappedInitializationException(Throwable cause) {
            super(cause);
        }

        public void rethrowCause() throws Exception {
            if (getCause() instanceof Exception) {
                throw (Exception)getCause();
            }

            throw this;
        }
    }

    protected View createGrailsView(String viewName) throws Exception {
        // try GSP if res is null

        GroovyObject controller = null;
        
        GrailsWebRequest webRequest = GrailsWebRequest.lookup();
        if(webRequest != null) {
            HttpServletRequest request = webRequest.getCurrentRequest();
            controller = webRequest.getAttributes().getController(request);
        }
        
        GroovyPageScriptSource scriptSource;
        if (controller == null) {
            scriptSource = groovyPageLocator.findViewByPath(viewName);
        }
        else {
            scriptSource = groovyPageLocator.findView(controller, viewName);
        }
        if (scriptSource != null) {
            return createGroovyPageView(scriptSource.getURI(), scriptSource);
        }

        return createFallbackView(viewName);
    }

    private View createGroovyPageView(String gspView, ScriptSource scriptSource) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Resolved GSP view at URI [" + gspView + "]");
        }
        GroovyPageView gspSpringView = new GroovyPageView();
        gspSpringView.setServletContext(getServletContext());
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
        if(resolveJspView) {
            return createJstlView(viewName);
        }
        return null;
    }
    
    protected View createJstlView(String viewName) throws Exception {
        AbstractUrlBasedView view = buildView(viewName);
        view.setApplicationContext(getApplicationContext());
        view.afterPropertiesSet();
        return view;
    }

    @Autowired(required=true)
    @Qualifier(GroovyPagesTemplateEngine.BEAN_ID)
    public void setTemplateEngine(GroovyPagesTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public long getCacheTimeout() {
        return cacheTimeout;
    }

    public void setCacheTimeout(long cacheTimeout) {
        this.cacheTimeout = cacheTimeout;
    }

    @Override
    public void clearCache() {
        super.clearCache();
        viewCache.clear();
    }

    public boolean isAllowGrailsViewCaching() {
        return allowGrailsViewCaching;
    }

    public void setAllowGrailsViewCaching(boolean allowGrailsViewCaching) {
        this.allowGrailsViewCaching = allowGrailsViewCaching;
    }

    public void setResolveJspView(boolean resolveJspView) {
        this.resolveJspView = resolveJspView;
    }
}
