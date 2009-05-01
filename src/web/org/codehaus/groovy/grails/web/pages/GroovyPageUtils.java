/* Copyright 2004-2005 Graeme Rocher
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

import groovy.lang.GroovyObject;
import grails.util.GrailsNameUtils;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;

/**
 * Methods to establish template names, paths and so on.
 *
 * @author Graeme Rocher
 * @since 1.1.1
 *
 *        <p/>
 *        Created: May 1, 2009
 */
public class GroovyPageUtils {
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
    public static String getTemplateURI(GroovyObject controller, String templateName) {
        if(controller == null) throw new IllegalArgumentException("Argument [controller] cannot be null");
        return getTemplateURI(getLogicalName(controller),templateName);
    }

    /**
    * Obtains a view URI of the given controller and view name
    * @param controller The name of the controller
    * @param viewName The name of the view
    * @return The view URI
    */
    public static String getViewURI(GroovyObject controller, String viewName) {
        if(controller == null) throw new IllegalArgumentException("Argument [controller] cannot be null");
        return getViewURI(getLogicalName(controller),viewName);

    }

    private static String getLogicalName(GroovyObject controller) {
        return GrailsNameUtils.getLogicalPropertyName(controller.getClass().getName(), ControllerArtefactHandler.TYPE);
    }

    /**
     * Obtains the URI to a template using the controller name and template name
     * @param controllerName The controller name
     * @param templateName The template name
     * @return The template URI
     */
    public static String getTemplateURI(String controllerName, String templateName) {
        FastStringWriter buf = new FastStringWriter();

        if(templateName.startsWith(SLASH_STR)) {
            String tmp = templateName.substring(1,templateName.length());
            if(tmp.indexOf(SLASH) > -1) {
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
        }
        else {
            String pathToTemplate = BLANK;

            int lastSlash = templateName.lastIndexOf(SLASH);
            if (lastSlash > -1) {
                pathToTemplate = templateName.substring(0, lastSlash + 1);
                templateName = templateName.substring(lastSlash + 1);
            }
            buf
                .append(SLASH)
                .append(controllerName)
                .append(SLASH)
                .append(pathToTemplate)
                .append(UNDERSCORE)
                .append(templateName);
        }
        return buf
                    .append(GroovyPage.EXTENSION)
                    .toString();

    }
    /**
     * Obtains a view URI of the given controller name and view name
     * @param controllerName The name of the controller
     * @param viewName The name of the view
     * @return The view URI
     */
    public static String getViewURI(String controllerName, String viewName) {
        FastStringWriter buf = new FastStringWriter();

        if(viewName.startsWith(SLASH_STR)) {
            String tmp = viewName.substring(1,viewName.length());
            if(tmp.indexOf(SLASH) > -1) {
                buf.append(SLASH);
                buf.append(tmp.substring(0,tmp.lastIndexOf(SLASH)));
                buf.append(SLASH);
                buf.append(tmp.substring(tmp.lastIndexOf(SLASH) + 1,tmp.length()));
            }
            else {
                buf.append(SLASH);
                buf.append(viewName.substring(1,viewName.length()));
            }
        }
        else {
            buf
              .append(SLASH)
              .append(controllerName)
              .append(SLASH)
              .append(viewName);

        }
        return buf
                    .append(GroovyPage.SUFFIX)
                    .toString();

    }
}
