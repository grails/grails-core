/*
 * Copyright 2004-2005 Graeme Rocher
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

import grails.util.GrailsNameUtils;
import groovy.lang.GroovyObject;
import groovy.lang.MissingPropertyException;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.springframework.util.Assert;

import javax.servlet.ServletRequest;

/**
 * Methods to establish template names, paths and so on.
 *
 * @author Graeme Rocher
 * @since 1.2
 */
public class GroovyPagesUriSupport implements GroovyPagesUriService {

    public static final String PATH_TO_VIEWS = "/WEB-INF/grails-app/views";
    private static final char SLASH = '/';
    private static final String SLASH_STR = "/";
    private static final String SLASH_UNDR = "/_";
    private static final String BLANK = "";
    private static final String UNDERSCORE = "_";

    /**
     * Obtains a template URI for the given controller instance and template name
     * @param controller The controller instance
     * @param templateName The template name
     * @return The template URI
     */
    public String getTemplateURI(GroovyObject controller, String templateName) {
        Assert.notNull(controller, "Argument [controller] cannot be null");
        return getTemplateURI(getLogicalControllerName(controller),templateName);
    }

    public void clear() {
        // do nothing
    }

    /**
     * Obtains a view URI of the given controller and view name
     * @param controller The name of the controller
     * @param viewName The name of the view
     * @return The view URI
     */
    public String getViewURI(GroovyObject controller, String viewName) {
        Assert.notNull(controller, "Argument [controller] cannot be null");
        return getViewURI(getLogicalControllerName(controller), viewName);
    }

    /**
     * Obtains a view URI of the given controller and view name without the suffix
     * @param controller The name of the controller
     * @param viewName The name of the view
     * @return The view URI
     */
    public String getNoSuffixViewURI(GroovyObject controller, String viewName) {
        Assert.notNull(controller, "Argument [controller] cannot be null");
        return getNoSuffixViewURI(getLogicalControllerName(controller), viewName);
    }

    public String getLogicalControllerName(GroovyObject controller) {
        ServletRequest request = null;
        try {
            request = (ServletRequest) controller.getProperty(ControllerDynamicMethods.REQUEST_PROPERTY);
        }
        catch (MissingPropertyException mpe) {
            // ignore
        }
        String logicalName = request != null ? (String) request.getAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE) : null;
        if (logicalName == null) {
            logicalName = GrailsNameUtils.getLogicalPropertyName(controller.getClass().getName(), ControllerArtefactHandler.TYPE);
        }
        return logicalName;
    }

    /**
     * Obtains the URI to a template using the controller name and template name
     * @param controllerName The controller name
     * @param templateName The template name
     * @return The template URI
     */
    public String getTemplateURI(String controllerName, String templateName) {

        if (templateName.startsWith(SLASH_STR)) {
            return getAbsoluteTemplateURI(templateName);
        }

        FastStringWriter buf = new FastStringWriter();
        String pathToTemplate = BLANK;

        int lastSlash = templateName.lastIndexOf(SLASH);
        if (lastSlash > -1) {
            pathToTemplate = templateName.substring(0, lastSlash + 1);
            templateName = templateName.substring(lastSlash + 1);
        }
        buf.append(SLASH)
           .append(controllerName)
           .append(SLASH)
           .append(pathToTemplate)
           .append(UNDERSCORE)
           .append(templateName);
        return buf.append(GroovyPage.EXTENSION).toString();
    }

    /**
     * Used to resolve template names that are not relative to a controller.
     *
     * @param templateName The template name normally beginning with /
     * @return The template URI
     */
    public String getAbsoluteTemplateURI(String templateName) {
        FastStringWriter buf = new FastStringWriter();
        String tmp = templateName.substring(1,templateName.length());
        if (tmp.indexOf(SLASH) > -1) {
            buf.append(SLASH);
            int i = tmp.lastIndexOf(SLASH);
            buf.append(tmp.substring(0, i));
            buf.append(SLASH_UNDR);
            buf.append(tmp.substring(i + 1,tmp.length()));
        }
        else {
            buf.append(SLASH_UNDR);
            buf.append(templateName.substring(1,templateName.length()));
        }
        String uri = buf.append(GroovyPage.EXTENSION).toString();
        buf.close();
        return uri;
    }

    /**
     * Obtains a view URI of the given controller name and view name
     * @param controllerName The name of the controller
     * @param viewName The name of the view
     * @return The view URI
     */
    public String getViewURI(String controllerName, String viewName) {
        FastStringWriter buf = new FastStringWriter();

        return getViewURIInternal(controllerName, viewName, buf, true);
    }

    /**
     * Obtains a view URI that is not relative to any given controller
     *
     * @param viewName The name of the view
     * @return The view URI
     */
    public String getAbsoluteViewURI(String viewName) {
        FastStringWriter buf = new FastStringWriter();
        return getAbsoluteViewURIInternal(viewName, buf, true);
    }

    /**
     * Obtains a view URI of the given controller name and view name without the suffix
     * @param controllerName The name of the controller
     * @param viewName The name of the view
     * @return The view URI
     */
    public String getNoSuffixViewURI(String controllerName, String viewName) {
        FastStringWriter buf = new FastStringWriter();

        return getViewURIInternal(controllerName, viewName, buf, false);
    }

    /**
     * Obtains a view URI when deployed within the /WEB-INF/grails-app/views context
     * @param controllerName The name of the controller
     * @param viewName The name of the view
     * @return The view URI
     */
    public String getDeployedViewURI(String controllerName, String viewName) {
        FastStringWriter buf = new FastStringWriter(PATH_TO_VIEWS);
        return getViewURIInternal(controllerName, viewName, buf, true);
    }

    /**
     * Obtains a view URI when deployed within the /WEB-INF/grails-app/views context
     * @param viewName The name of the view
     * @return The view URI
     */
    public String getDeployedAbsoluteViewURI(String viewName) {
        FastStringWriter buf = new FastStringWriter(PATH_TO_VIEWS);
        return getAbsoluteViewURIInternal(viewName, buf, true);
    }

    private String getViewURIInternal(String controllerName, String viewName, FastStringWriter buf, boolean includeSuffix) {
        if (viewName != null && viewName.startsWith(SLASH_STR)) {
            return getAbsoluteViewURIInternal(viewName, buf, includeSuffix);
        }

        buf.append(SLASH).append(controllerName);
        if (viewName != null) {
            buf.append(SLASH).append(viewName);
        }

        return includeSuffix ? buf.append(GroovyPage.SUFFIX).toString() : buf.toString();
    }

    private String getAbsoluteViewURIInternal(String viewName, FastStringWriter buf, boolean includeSuffix) {
        String tmp = viewName.substring(1,viewName.length());
        if (tmp.indexOf(SLASH) > -1) {
            buf.append(SLASH);
            buf.append(tmp.substring(0,tmp.lastIndexOf(SLASH)));
            buf.append(SLASH);
            buf.append(tmp.substring(tmp.lastIndexOf(SLASH) + 1,tmp.length()));
        }
        else {
            buf.append(SLASH);
            buf.append(viewName.substring(1,viewName.length()));
        }
        if (includeSuffix) {
            buf.append(GroovyPage.SUFFIX).toString();
        }
        return buf.toString();
    }
}
