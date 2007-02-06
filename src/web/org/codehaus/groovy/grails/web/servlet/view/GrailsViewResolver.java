/* Copyright 2004-2005 the original author or authors.
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

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.core.io.Resource;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.servlet.GrailsHttpServletRequest;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletContext;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.net.URL;

import grails.util.GrailsUtil;
import groovy.lang.GroovyObject;

/**
 * A Grails view resolver which evaluates the existance of a view for different extensions choosing which
 * one to delegate to.
 *
 * @author Graeme Rocher
 * @since 11-Jan-2006
 */
public class GrailsViewResolver extends InternalResourceViewResolver {
    private String localPrefix;
    private static final Log LOG = LogFactory.getLog(GrailsViewResolver.class);
    private static final String GSP_SUFFIX = ".gsp";
    private Map resolvedCache = new HashMap();


    public void setPrefix(String prefix) {
        super.setPrefix(prefix);
        this.localPrefix = prefix;
    }

    public void setSuffix(String suffix) {
        super.setSuffix(suffix);
    }

    protected View loadView(String viewName, Locale locale) throws Exception {
        AbstractUrlBasedView view = buildView(viewName);

        ServletContext context = getServletContext();
        URL res = context.getResource(view.getUrl());
        // try GSP if res is null
        if(res == null) {
            if(resolvedCache.containsKey(viewName)) {
        		view.setUrl(resolvedCache.get(viewName).toString());
        	}
        	else {

                GrailsWebRequest webRequest = (GrailsWebRequest)RequestContextHolder.currentRequestAttributes();

                GrailsHttpServletRequest request = webRequest.getCurrentRequest();
                GroovyObject controller = webRequest
                                                .getAttributes()
                                                .getController(request);

                GrailsApplication application = webRequest
                                                    .getAttributes()
                                                    .getGrailsApplication();


                String gspView;
                String pluginContextPath = "";
                // try to resolve the view relative to the controller first, this allows us to support
                // views provided by plugins
                if(controller != null && application != null) {
                    Resource controllerResource = application.getResourceForClass(controller.getClass());
                    Resource viewsDir = GrailsResourceUtils.getViewsDir(controllerResource);

                    String pathToView = GrailsResourceUtils.getRelativeInsideWebInf(viewsDir);
                    gspView = pathToView + viewName + GSP_SUFFIX;
                }
                else {
                    gspView = localPrefix + viewName + GSP_SUFFIX;
                }
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Attempting to resolve view for URI ["+gspView+"]");
                }
	            res = context.getResource(gspView);
	            if(res != null) {
	                view.setUrl(gspView);
                    if(!GrailsUtil.isDevelopmentEnv()) {
                        this.resolvedCache.put(viewName,gspView);
                    }                    
	            }
        	}
        }

        view.setApplicationContext(getApplicationContext());
        view.afterPropertiesSet();
        return view;
    }
}
