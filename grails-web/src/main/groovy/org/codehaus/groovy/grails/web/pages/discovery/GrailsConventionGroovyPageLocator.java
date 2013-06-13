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

import javax.servlet.http.HttpServletRequest;

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;
import org.codehaus.groovy.grails.web.mime.MimeType;
import org.codehaus.groovy.grails.web.mime.MimeTypeResolver;
import org.codehaus.groovy.grails.web.pages.DefaultGroovyPagesUriService;
import org.codehaus.groovy.grails.web.pages.GroovyPageBinding;
import org.codehaus.groovy.grails.web.pages.GroovyPagesUriService;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Extended GroovyPageLocator that deals with the details of Grails' conventions
 * for controllers names, view names and template names
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class GrailsConventionGroovyPageLocator extends DefaultGroovyPageLocator {

    private static final char DOT = '.';

    private GroovyPagesUriService uriService = new DefaultGroovyPagesUriService();

    private MimeTypeResolver mimeTypeResolver;

    @Autowired(required = false)
    public void setMimeTypeResolver(MimeTypeResolver mimeTypeResolver) {
        this.mimeTypeResolver = mimeTypeResolver;
    }

    /**
     * Find a view for a path. For example /foo/bar will search for /WEB-INF/grails-app/views/foo/bar.gsp in production
     * and grails-app/views/foo/bar.gsp at development time
     *
     * @param uri The uri to search
     * @return The script source
     */
    public GroovyPageScriptSource findViewByPath(String uri) {
        return uri == null ? null : findPage(uriService.getAbsoluteViewURI(uri));
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
        return findView(controllerName, viewName, lookupRequestFormat());
    }

    public GroovyPageScriptSource findView(String controllerName, String viewName, String format) {
        GroovyPageScriptSource scriptSource = findViewForFormat(controllerName, viewName, format);
        if (scriptSource == null) {
            scriptSource  = findPage(uriService.getViewURI(controllerName, viewName));
        }

        return scriptSource;
    }

    /**
     * Finds a view for the given view name and format, otherwise returns null if it doesn't exist
     * @param controllerName The controller name
     * @param viewName The view name
     * @param format The format
     * @return The script source or null
     */
    public GroovyPageScriptSource findViewForFormat(String controllerName, String viewName, String format) {
        String viewNameWithFormat = getViewNameWithFormat(viewName, format);
        return findPage(uriService.getViewURI(controllerName, viewNameWithFormat));
    }

    public String resolveViewFormat(String viewName) {
        String format = lookupRequestFormat();
        return getViewNameWithFormat(viewName, format);
    }

    private String getViewNameWithFormat(String viewName, String format) {
        if (format == null) {
            return viewName;
        }
        return viewName + DOT + format;
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
        if (controller == null || viewName == null) {
            return null;
        }

        String controllerName = getNameForController(controller);
        String viewNameWithFormat = resolveViewFormat(viewName);

        GroovyPageScriptSource scriptSource = findPage(uriService.getViewURI(controllerName, viewNameWithFormat));
        if (scriptSource == null) {
            scriptSource = findPage(uriService.getViewURI(controllerName, viewName));
        }
        if (scriptSource == null && pluginManager != null) {
            String pathToView = pluginManager.getPluginViewsPathForInstance(controller);
            if (pathToView != null) {
                scriptSource = findViewByPath(GrailsResourceUtils.appendPiecesForUri(pathToView,viewName));
            }
        }
        // if all else fails do a full search
        if (scriptSource == null) {
            scriptSource = findView(controllerName, viewName);
        }
        return scriptSource;
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
        if (webRequest == null) {
            return findViewByPath(viewName);
        }
        return findView(webRequest.getControllerName(), viewName);
    }
    /**
     * Finds a template for the given given template name, looking up the controller from the request as necessary
     *
     * @param templateName The template name
     * @return The GroovyPageScriptSource
     */
    public GroovyPageScriptSource findTemplate(String templateName) {
        GrailsWebRequest webRequest = GrailsWebRequest.lookup();
        if (webRequest == null) {
            return findTemplateByPath(templateName);
        }
        return findTemplate(webRequest.getControllerName(), templateName);
    }

    /**
     * Finds a template for the given given template name, looking up the controller from the request as necessary
     *
     * @param templateName The template name
     * @return The GroovyPageScriptSource
     */
    public GroovyPageScriptSource findTemplateInBinding(String templateName, GroovyPageBinding binding) {
        GrailsWebRequest webRequest = GrailsWebRequest.lookup();
        if (webRequest == null) {
            return findPageInBinding(uriService.getAbsoluteTemplateURI(templateName), binding);
        }
        return findPageInBinding(uriService.getTemplateURI(webRequest.getControllerName(), templateName), binding);
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
        if (webRequest == null) {
            return findPageInBinding(pluginName, uriService.getAbsoluteTemplateURI(templateName), binding);
        }
        return findPageInBinding(pluginName, uriService.getTemplateURI(webRequest.getControllerName(), templateName), binding);
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

    protected String lookupRequestFormat() {
        if(mimeTypeResolver != null) {
            MimeType mimeType = mimeTypeResolver.resolveResponseMimeType();

            if(mimeType != null) {
                return mimeType.getExtension();
            }
        }
        else {
            GrailsWebRequest webRequest  =
                GrailsWebRequest.lookup();
            if(webRequest != null) {

                HttpServletRequest request = webRequest.getCurrentRequest();
                Object format = request.getAttribute(GrailsApplicationAttributes.RESPONSE_FORMAT);
                return format == null ? null : format.toString();
            }
        }
        return null;
    }

    protected String getNameForController(Object controller) {
        return GrailsNameUtils.getLogicalPropertyName(controller.getClass().getName(), ControllerArtefactHandler.TYPE);
    }
}
