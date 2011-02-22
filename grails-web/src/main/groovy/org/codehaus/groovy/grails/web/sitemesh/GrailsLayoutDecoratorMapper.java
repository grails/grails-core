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
package org.codehaus.groovy.grails.web.sitemesh;

import grails.util.Environment;
import grails.util.Metadata;
import grails.util.PluginBuildSettings;
import groovy.lang.GroovyObject;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils;
import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods;
import org.codehaus.groovy.grails.web.pages.GroovyPageResourceLoader;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.opensymphony.module.sitemesh.Config;
import com.opensymphony.module.sitemesh.Decorator;
import com.opensymphony.module.sitemesh.DecoratorMapper;
import com.opensymphony.module.sitemesh.Page;
import com.opensymphony.module.sitemesh.mapper.AbstractDecoratorMapper;
import com.opensymphony.module.sitemesh.mapper.DefaultDecorator;

/**
 * Implements the SiteMesh decorator mapper interface and allows grails views to map to grails layouts.
 *
 * @author Graeme Rocher
 */
public class GrailsLayoutDecoratorMapper extends AbstractDecoratorMapper implements DecoratorMapper {

    private static final String DEFAULT_DECORATOR_PATH = GrailsApplicationAttributes.PATH_TO_VIEWS + "/layouts";
    private static final String DEFAULT_VIEW_TYPE = ".gsp";
    private static final Log LOG = LogFactory.getLog( GrailsLayoutDecoratorMapper.class );

    private Map<String, Decorator> decoratorMap = new ConcurrentHashMap<String, Decorator>();
    private ServletContext servletContext;
    private WebApplicationContext applicationContext;
    private GrailsPluginManager pluginManager;
    private PluginBuildSettings pluginBuildSettings;

    @Override
    public void init(Config c, Properties properties, DecoratorMapper parentMapper) throws InstantiationException {
        super.init(c, properties, parentMapper);
        servletContext = c.getServletContext();
        applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        if (applicationContext.containsBean(GrailsPluginManager.BEAN_NAME)) {
            pluginManager = applicationContext.getBean(GrailsPluginManager.BEAN_NAME, GrailsPluginManager.class);
            pluginBuildSettings = pluginManager.getPluginBuildSettings();
        }
    }

    @Override
    public Decorator getDecorator(HttpServletRequest request, Page page) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Evaluating layout for request: " + request.getRequestURI());
        }
        String layoutName = page.getProperty("meta.layout");

        if (StringUtils.isBlank(layoutName)) {
            GroovyObject controller = (GroovyObject)request.getAttribute(GrailsApplicationAttributes.CONTROLLER);
            if (controller != null) {

                String controllerName = (String)controller.getProperty(ControllerDynamicMethods.CONTROLLER_NAME_PROPERTY);
                String actionUri = (String)controller.getProperty(ControllerDynamicMethods.ACTION_URI_PROPERTY);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Found controller in request, location layout for controller [" +
                            controllerName + "] and action [" + actionUri + "]");
                }

                Decorator d = null;

                Object layoutProperty = GrailsClassUtils.getStaticPropertyValue(controller.getClass(), "layout");
                if (layoutProperty instanceof String) {
                    LOG.debug("layout property found in controller, looking for template named " + layoutProperty);
                    d = getNamedDecorator(request, (String) layoutProperty);
                }

                if (d == null) {
                    d = getNamedDecorator(request, actionUri.substring(1));
                }

                if (d != null) {
                    return d;
                }

                if (!StringUtils.isBlank(controllerName)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Action layout not found, trying controller");
                    }

                    d = getNamedDecorator(request, controllerName);
                    if (d != null) {
                        return d;
                    }

                    d = getApplicationDefaultDecorator(request);
                    if (d != null) {
                        return d;
                    }
                    return parent != null ? super.getDecorator(request, page) : null;
                }
            }
            else {
                Decorator d = getApplicationDefaultDecorator(request);
                if (d != null) {
                    return d;
                }
                return parent != null ? super.getDecorator(request, page) : null;
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Evaluated layout for page: " + layoutName);
        }

        Decorator d = getNamedDecorator(request, layoutName);
        if (d != null) {
            return d;
        }

        return parent != null ? super.getDecorator(request, page) : null;
    }

    @SuppressWarnings("rawtypes")
    protected Decorator getApplicationDefaultDecorator(HttpServletRequest request) {
        String defaultDecoratorName = null;
        Map conf = ConfigurationHolder.getFlatConfig();
        if (conf != null && conf.containsKey("grails.sitemesh.default.layout")) {
            defaultDecoratorName = conf.get("grails.sitemesh.default.layout").toString();
        }
        else {
            defaultDecoratorName = "application";
        }
        return getNamedDecorator(request, defaultDecoratorName);
    }

    @Override
    public Decorator getNamedDecorator(HttpServletRequest request, String name) {
        if (StringUtils.isBlank(name)) return null;

        if (Environment.getCurrent()!= Environment.DEVELOPMENT && decoratorMap.containsKey(name)) {
            return decoratorMap.get(name);
        }

        String decoratorName = name;
        if (!name.matches("(.+)(\\.)(\\w{2}|\\w{3})")) {
            name += DEFAULT_VIEW_TYPE;
        }
        String decoratorPage = DEFAULT_DECORATOR_PATH + '/' + name;

        ResourceLoader resourceLoader = establishResourceLoader();

        // lookup something like /WEB-INF/grails-app/views/layouts/[NAME].gsp
        Resource res = resourceLoader.getResource(decoratorPage);
        Decorator d = null;
        if (!res.exists()) {
            // lookup something like /WEB-INF/plugins/myplugin/grails-app/views/layouts/[NAME].gsp
            String pathToView = lookupPathToControllerView(request, name);
            res = pathToView != null ? resourceLoader.getResource(pathToView) : null;
            if (res != null && res.exists()) {
                decoratorPage = pathToView;
                d = useExistingDecorator(request, decoratorName, decoratorPage);
            }
            else {
                // scan /WEB-INF/plugins/*/grails-app/views/layouts/[NAME].gsp for first matching
                final String pluginViewLocation = searchPluginViews(name, resourceLoader);
                if (pluginViewLocation!= null) {
                    decoratorPage = pluginViewLocation;
                    d = useExistingDecorator(request, decoratorName, decoratorPage);
                }
            }
        }
        else {
            d = useExistingDecorator(request, decoratorName, decoratorPage);
        }
        return d;
    }

    public String searchPluginViews(String name, ResourceLoader resourceLoader) {
        if (Metadata.getCurrent().isWarDeployed()) {
            return searchPluginViewsForWarDeployed(name, resourceLoader);
        }

        return searchPluginViewsInDevelopmentMode(name);
    }

    private String searchPluginViewsInDevelopmentMode(String name) {

        String pluginViewLocation = null;
        if(pluginBuildSettings != null) {
            for (Resource resource : pluginBuildSettings.getPluginDirectories()) {
                try {
                    final String pathToLayoutInPlugin = "grails-app/views/layouts/"+name;
                    final String absolutePathToResource = resource.getFile().getAbsolutePath();
                    if (!absolutePathToResource.endsWith("/")) {
                        resource = new FileSystemResource(absolutePathToResource + '/');
                    }
                    final Resource layoutPath = resource.createRelative(pathToLayoutInPlugin);
                    if (layoutPath.exists()) {
                        GrailsPluginInfo info = pluginBuildSettings.getPluginInfo(absolutePathToResource);
                        pluginViewLocation = GrailsResourceUtils.WEB_INF + "/plugins/" + info.getFullName() + '/' + pathToLayoutInPlugin;
                    }
                }
                catch (IOException e) {
                    // ignore
                }
            }
        	
        }
        return pluginViewLocation;
    }

    private String searchPluginViewsForWarDeployed(String name, ResourceLoader resourceLoader) {
        String pluginViewLocation = null;
        ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver(resourceLoader);
        try {
            Resource[] resources = resourceResolver.getResources(GrailsResourceUtils.WEB_INF + "/plugins/*/" + GrailsResourceUtils.GRAILS_APP_DIR + "/views/layouts/" + name);
            if (resources.length > 0 && resources[0].exists()) {
                Resource r = resources[0];
                String url = r.getURL().toString();
                pluginViewLocation = GrailsResourceUtils.WEB_INF + url.substring(url.indexOf("/plugins"));
            }
        }
        catch (Exception e) {
            // ignore
        }
        return pluginViewLocation;
    }

    private String lookupPathToControllerView(HttpServletRequest request, String viewName) {
        GrailsWebRequest webRequest = GrailsWebRequest.lookup(request);
        if (webRequest != null) {
            GroovyObject controller = webRequest.getAttributes().getController(request);

            if (controller != null && pluginManager != null) {
                String pathToView = pluginManager.getPluginViewsPathForInstance(controller);
                return GrailsResourceUtils.WEB_INF + (pathToView != null ? pathToView : "") + "/layouts/" + viewName;
            }
        }
        return null;
    }

    private Decorator useExistingDecorator(HttpServletRequest request, String decoratorName, String decoratorPage) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Using decorator " + decoratorPage);
        }

        Decorator d =  new DefaultDecorator(decoratorName,decoratorPage,request.getRequestURI(), Collections.EMPTY_MAP);
        decoratorMap.put(decoratorName,d);
        return d;
    }

    private ResourceLoader establishResourceLoader() {
        ApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        GrailsApplication application = null;
        if (ctx.containsBean(GrailsApplication.APPLICATION_ID)) {
            application = (GrailsApplication)ctx.getBean(GrailsApplication.APPLICATION_ID);
        }

        if (application == null) {
            return ctx;
        }

        if (ctx.containsBean(GroovyPageResourceLoader.BEAN_ID) && !application.isWarDeployed()) {
            return (ResourceLoader)ctx.getBean(GroovyPageResourceLoader.BEAN_ID);
        }

        return ctx;
    }
}
