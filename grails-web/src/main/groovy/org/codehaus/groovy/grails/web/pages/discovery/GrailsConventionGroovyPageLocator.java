/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.web.pages.discovery;

import grails.util.GrailsNameUtils;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;
import org.codehaus.groovy.grails.plugins.BinaryGrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.web.pages.DefaultGroovyPagesUriService;
import org.codehaus.groovy.grails.web.pages.GroovyPageBinding;
import org.codehaus.groovy.grails.web.pages.GroovyPagesUriService;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;

import javax.servlet.http.HttpServletRequest;

/**
 * Extended GroovyPageLocator that deals with the details of Grails' conventions for controllers names, view names and template names
 *
 * @author Graeme Rocher
 * @since 1.4
 */
public class GrailsConventionGroovyPageLocator extends DefaultGroovyPageLocator {

    private static final char DOT = '.';
    private static final String LAYOUTS_PATH = "/layouts";
    private GroovyPagesUriService uriService = new DefaultGroovyPagesUriService();

    /**
     * Find a view for a path. For example /foo/bar will search for /WEB-INF/grails-app/views/foo/bar.gsp in production
     * and grails-app/views/foo/bar.gsp at development time
     *
     * @param uri The uri to search
     * @return The script source
     */
    public GroovyPageScriptSource findViewByPath(String uri) {
        return findPage(uriService.getAbsoluteViewURI(uri));
    }


    /**
     * <p>Finds a layout by name. For example the layout name "main" will search for /WEB-INF/grails-app/views/layouts/main.gsp in production
     * and  grails-app/views/layouts/main.gsp at development time</p>
     *
     * <p>If the layout is not found in the application then a scan is executed that searches through binary and source plugins looking for the first matching layout name</p>
     *
     * @param layoutName The name of the layout
     * @return The script source or null
     */
    public GroovyPageScriptSource findLayout(String layoutName) {
        GroovyPageScriptSource scriptSource = findPage(uriService.getAbsoluteViewURI(GrailsResourceUtils.appendPiecesForUri(LAYOUTS_PATH, layoutName)));
        if(scriptSource == null) {
            scriptSource = findLayoutInBinaryPlugins(layoutName);
        }
        return scriptSource;
    }

    /**
     * <p>Finds a layout by name. For example the layout name "main" will search for /WEB-INF/grails-app/views/layouts/main.gsp in production
     * and  grails-app/views/layouts/main.gsp at development time</p>
     *
     * <p>If the layout is not found in the application then a scan is executed that searches through binary and source plugins looking for the first matching layout name</p>
     *
     * @param controller The controller
     * @param layoutName The layout
     *
     * @return The script source or null
     */
    public GroovyPageScriptSource findLayout(Object controller, String layoutName) {
        GroovyPageScriptSource scriptSource = findLayout(layoutName);
        if(scriptSource == null && controller != null) {
            if (pluginManager != null) {
                String pathToView = pluginManager.getPluginViewsPathForInstance(controller);
                if(pathToView != null) {
                     scriptSource = findPage(uriService.getAbsoluteViewURI(GrailsResourceUtils.appendPiecesForUri(pathToView, LAYOUTS_PATH, layoutName)));
                }
            }
        }
        return scriptSource;
    }


    /**
     * <p>Finds a view for the given controller name and view name. For example specifying a controller name of "home" and a view name of "index" will search for
     * /WEB-INF/grails-app/views/home/index.gsp in production and grails-app/views/home/index.gsp in development</p>
     *
     * <p>This method will also detect the presence of the requested response format and try to resolve a more appropriate view. For example in the response format
     * is 'xml' then /WEB-INF/grails-app/views/home/index.xml.gsp will be tried first</p>
     *
     * <p>If the view is not found in the application then a scan is executed that searches through binary and source plugins looking for the first matching view name</p>
     *
     * @param controllerName The controller name
     * @param viewName The view name
     * @return The GroovyPageScriptSource
     */
    public GroovyPageScriptSource findView(String controllerName, String viewName) {

        String viewNameWithFormat = resolveViewFormat(viewName);
        GroovyPageScriptSource scriptSource = findPage(uriService.getViewURI(controllerName, viewNameWithFormat));
        if(scriptSource == null) {
            scriptSource  = findPage(uriService.getViewURI(controllerName, viewName));
        }


        return scriptSource;
    }

    private String resolveViewFormat(String viewName) {
        String format = lookupRequestFormat();
        if (format != null) {
              return viewName + DOT + format;
        }
        return viewName;
    }

    /**
     * <p>Finds a view for the given controller and view name. For example specifying a controller with a class name of HomeController and a view name of "index" will search for
     * /WEB-INF/grails-app/views/home/index.gsp in production and grails-app/views/home/index.gsp in development</p>
     *
     * <p>If the view is not found in the application then a scan is executed that searches through binary and source plugins looking for the first matching view name</p>
     *
     * @param controller The controller
     * @param viewName The view name
     * @return The GroovyPageScriptSource
     */
    public GroovyPageScriptSource findView(Object controller, String viewName) {
        if(controller != null && viewName != null) {
            String controllerName = getNameForController(controller);
            String viewNameWithFormat = resolveViewFormat(viewName);

            GroovyPageScriptSource scriptSource = findResourceScriptSource(uriService.getViewURI(controllerName, viewNameWithFormat));
            if(scriptSource == null) {
                scriptSource = findResourceScriptSource(uriService.getViewURI(controllerName, viewName));
            }
            if(scriptSource == null && pluginManager != null) {
                String pathToView = pluginManager.getPluginViewsPathForInstance(controller);
                if(pathToView != null) {
                    scriptSource = findViewByPath(GrailsResourceUtils.appendPiecesForUri(pathToView,viewName));
                }
            }
            // if all else fails do a full search
            if(scriptSource == null) {
                scriptSource = findView(controllerName, viewName);
            }
            return scriptSource;
        }
        return null;
    }

    /**
     * Finds a template for the given controller name and template name
     *
     * @param controllerName The controller name
     * @param templateName The view name
     * @return The GroovyPageScriptSource
     */
    public GroovyPageScriptSource findTemplate(String controllerName, String templateName) {
        return findPage(uriService.getTemplateURI(controllerName, templateName));
    }

    /**
     * Finds a template for the given controller name and template name
     *
     * @param controller The controller n
     * @param templateName The view name
     * @return The GroovyPageScriptSource
     */
    public GroovyPageScriptSource findTemplate(Object controller, String templateName) {
        String controllerName = getNameForController(controller);
        return findPage(uriService.getTemplateURI(controllerName, templateName));
    }

    /**
     * Finds a view for the given given view name, looking up the controller from the request as necessary
     *
     * @param viewName The view name
     * @return The GroovyPageScriptSource
     */
    public GroovyPageScriptSource findView(String viewName) {
        GrailsWebRequest webRequest = GrailsWebRequest.lookup();
        if(webRequest != null) {
            return findView(webRequest.getControllerName(), viewName);
        }
        return findViewByPath(viewName);
    }
    /**
     * Finds a template for the given given template name, looking up the controller from the request as necessary
     *
     * @param templateName The template name
     * @return The GroovyPageScriptSource
     */
    public GroovyPageScriptSource findTemplate(String templateName) {
        GrailsWebRequest webRequest = GrailsWebRequest.lookup();
        if(webRequest != null) {
            return findTemplate(webRequest.getControllerName(), templateName);
        }
        return findTemplateByPath(templateName);
    }

    /**
     * Finds a template for the given given template name, looking up the controller from the request as necessary
     *
     * @param templateName The template name
     * @return The GroovyPageScriptSource
     */
    public GroovyPageScriptSource findTemplateInBinding(String templateName, GroovyPageBinding binding) {
        GrailsWebRequest webRequest = GrailsWebRequest.lookup();
        if(webRequest != null) {
            return findPageInBinding(uriService.getTemplateURI(webRequest.getControllerName(), templateName), binding);
        }
        return findPageInBinding(uriService.getAbsoluteTemplateURI(templateName), binding);
    }

    /**
     * Finds a template for the given given template name, looking up the controller from the request as necessary
     *
     * @param pluginName The plugin
     * @param templateName The template name
     * @param binding The binding
     * @return The GroovyPageScriptSource
     */
    public GroovyPageScriptSource findTemplateInBinding(String pluginName, String templateName, GroovyPageBinding binding) {
        GrailsWebRequest webRequest = GrailsWebRequest.lookup();
        if(webRequest != null) {
            return findPageInBinding(pluginName, uriService.getTemplateURI(webRequest.getControllerName(), templateName), binding);
        }
        return findPageInBinding(pluginName, uriService.getAbsoluteTemplateURI(templateName), binding);
    }

    /**
     * Find a template for a path. For example /foo/bar will search for /WEB-INF/grails-app/views/foo/_bar.gsp in production
     * and grails-app/views/foo/_bar.gsp at development time
     *
     * @param uri The uri to search
     * @return The script source
     */
    public GroovyPageScriptSource findTemplateByPath(String uri) {
        return findPage(uriService.getAbsoluteTemplateURI(uri));
    }

    private String lookupRequestFormat() {
        GrailsWebRequest webRequest = GrailsWebRequest.lookup();
        if(webRequest != null) {
            HttpServletRequest request = webRequest.getCurrentRequest();
            return request.getAttribute(GrailsApplicationAttributes.RESPONSE_FORMAT) != null ?
                    request.getAttribute(GrailsApplicationAttributes.RESPONSE_FORMAT).toString() : null;

        }
        return null;
    }

    protected String getNameForController(Object controller) {
        return GrailsNameUtils.getLogicalPropertyName(controller.getClass().getName(), ControllerArtefactHandler.TYPE);
    }

    protected GroovyPageScriptSource findLayoutInBinaryPlugins(String name) {
        if (pluginManager != null) {
            final GrailsPlugin[] allPlugins = pluginManager.getAllPlugins();
            for (GrailsPlugin plugin : allPlugins) {
                if (plugin instanceof BinaryGrailsPlugin) {
                    BinaryGrailsPlugin binaryGrailsPlugin = (BinaryGrailsPlugin) plugin;

                    String uri = "/WEB-INF/grails-app/views/layouts/" + name;
                    Class<?> viewClass = binaryGrailsPlugin.resolveView(uri);
                    if (viewClass != null) {
                        return new GroovyPageCompiledScriptSource(uri, viewClass);
                    }
                }
            }
        }
        return null;
    }
}
