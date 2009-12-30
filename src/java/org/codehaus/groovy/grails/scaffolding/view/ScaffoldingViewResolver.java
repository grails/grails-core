/* Copyright 2008-2009 Graeme Rocher
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
package org.codehaus.groovy.grails.scaffolding.view;

import grails.util.GrailsUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.scaffolding.GrailsTemplateGenerator;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.servlet.view.GrailsViewResolver;
import org.codehaus.groovy.grails.web.servlet.view.GroovyPageView;
import org.codehaus.groovy.grails.web.util.WebUtils;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.servlet.View;

import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Overrides the default Grails view resolver and resolves scaffolded views at runtime
 *
 * @author Graeme Rocher
 * @since 1.1
 *        <p/>
 *        Created: Nov 24, 2008
 */
public class ScaffoldingViewResolver extends GrailsViewResolver implements ApplicationContextAware{

    GrailsTemplateGenerator templateGenerator;
    Map scaffoldedActionMap = Collections.EMPTY_MAP;
    Map scaffoldedDomains = Collections.EMPTY_MAP;

    static final Map<ViewKey, View> scaffoldedViews = new ConcurrentHashMap<ViewKey, View>();
    static final Log LOG = LogFactory.getLog(ScaffoldingViewResolver.class);

    /**
     * Clears any cached scaffolded views
     */
    public static void clearViewCache() {
        scaffoldedViews.clear();
    }

    public void setTemplateGenerator(GrailsTemplateGenerator templateGenerator) {
        this.templateGenerator = templateGenerator;
    }

    public void setScaffoldedActionMap(Map scaffoldedActionMap) {
        this.scaffoldedActionMap = scaffoldedActionMap;
    }

    public void setScaffoldedDomains(Map scaffoldedDomains) {
        this.scaffoldedDomains = scaffoldedDomains;
    }

    @Override
    protected View loadView(String viewName, Locale locale) throws Exception {
        final View resolvedView = super.loadView(viewName, locale);
        if(templateGenerator == null || resolvedView instanceof GroovyPageView) {
            return resolvedView;
        }
        else {
            GrailsWebRequest webRequest = WebUtils.retrieveGrailsWebRequest();
            List controllerActions = (List) scaffoldedActionMap.get(webRequest.getControllerName());
            if(controllerActions != null && controllerActions.contains(webRequest.getActionName())) {
                GrailsDomainClass domainClass = (GrailsDomainClass) scaffoldedDomains.get(webRequest.getControllerName());
                if(domainClass!=null) {
                    String viewFileName;
                    final int i = viewName.lastIndexOf('/');
                    if(i > -1) {
                        viewFileName = viewName.substring(i, viewName.length());
                    }
                    else {
                        viewFileName = viewName;
                    }
                    final ViewKey viewKey = new ViewKey(webRequest.getControllerName(), viewFileName);
                    View v = scaffoldedViews.get(viewKey);
                    if( v == null) {
                        String viewCode = null;
                        try {
                            viewCode = generateViewSource(webRequest, domainClass);
                        }
                        catch (Exception e) {
                            GrailsUtil.deepSanitize(e);
                            LOG.error("Error generating scaffolded view [" + viewName + "]: " + e.getMessage(),e);        
                            return resolvedView;
                        }
                        v = createScaffoldedView(viewName, viewCode);
                        scaffoldedViews.put(viewKey, v);
                    }
                    if(v!=null) return v;

                }
            }
            
        }
        return resolvedView;
    }

    protected View createScaffoldedView(String viewName, String viewCode) {
        final ScaffoldedGroovyPageView view = new ScaffoldedGroovyPageView(viewName, viewCode);
        view.setApplicationContext(getApplicationContext());
        view.setServletContext(getServletContext());
        view.setTemplateEngine(templateEngine);
        return view;
    }

    protected String generateViewSource(GrailsWebRequest webRequest, GrailsDomainClass domainClass) {
        StringWriter sw = new StringWriter();
        templateGenerator.generateView(domainClass,webRequest.getActionName(),sw);
        return sw.toString();
    }

    private class ViewKey {
        private String controller;
        private String action;

        ViewKey(String controller, String action) {
            this.controller = controller;
            this.action = action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ViewKey viewKey = (ViewKey) o;

            if (action != null ? !action.equals(viewKey.action) : viewKey.action != null) return false;
            if (!controller.equals(viewKey.controller)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = controller.hashCode();
            result = 31 * result + (action != null ? action.hashCode() : 0);
            return result;
        }
    }
}

