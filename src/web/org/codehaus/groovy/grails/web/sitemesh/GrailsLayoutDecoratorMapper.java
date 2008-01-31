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

import com.opensymphony.module.sitemesh.Config;
import com.opensymphony.module.sitemesh.Decorator;
import com.opensymphony.module.sitemesh.DecoratorMapper;
import com.opensymphony.module.sitemesh.Page;
import com.opensymphony.module.sitemesh.mapper.AbstractDecoratorMapper;
import com.opensymphony.module.sitemesh.mapper.DefaultDecorator;
import groovy.lang.GroovyObject;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods;
import org.codehaus.groovy.grails.web.pages.GroovyPageResourceLoader;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Implements the SiteMesh decorator mapper interface and allows grails views to map to grails layouts
 *  
 * @author Graeme Rocher
 * @since Oct 10, 2005
 */
public class GrailsLayoutDecoratorMapper extends AbstractDecoratorMapper implements DecoratorMapper {

	private static final String DEFAULT_DECORATOR_PATH = GrailsApplicationAttributes.PATH_TO_VIEWS+"/layouts";
	private static final String DEFAULT_VIEW_TYPE = ".gsp";
	
	private static final Log LOG = LogFactory.getLog( GrailsLayoutDecoratorMapper.class );
	
	
	private Map decoratorMap = new HashMap();
	private ServletContext servletContext;
	
	public void init(Config config, Properties properties, DecoratorMapper parent) throws InstantiationException {		
		super.init(config,properties,parent);
		this.servletContext = config.getServletContext();
        
    }

	public Decorator getDecorator(HttpServletRequest request, Page page) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("Evaluating layout for request: " + request.getRequestURI());
		}			
		String layoutName = page.getProperty("meta.layout");
		
		if(StringUtils.isBlank(layoutName)) {		
			GroovyObject controller = (GroovyObject)request.getAttribute(GrailsApplicationAttributes.CONTROLLER);		
			if(controller != null) {

				String controllerName = (String)controller.getProperty(ControllerDynamicMethods.CONTROLLER_NAME_PROPERTY);
				String actionUri = (String)controller.getProperty(ControllerDynamicMethods.ACTION_URI_PROPERTY);

                if(LOG.isDebugEnabled())
                    LOG.debug("Found controller in request, location layout for controller ["+controllerName+"] and action ["+actionUri+"]");


                Decorator d = getNamedDecorator(request, actionUri.substring(1));
			    if(d!=null) {
			    	return d;
			    } else if(!StringUtils.isBlank(controllerName)) {
					if(LOG.isDebugEnabled())
						LOG.debug("Action layout not found, trying controller");
					
					d = getNamedDecorator(request, controllerName);
					if(d != null) {
						return d;
					}
					else {
						return parent != null ? super.getDecorator(request, page) : null;	
					}			    	
									    	
			    }					

			}
			else {
				return parent != null ? super.getDecorator(request, page) : null;			
			}			
		}					
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("Evaluated layout for page: " + layoutName);
		}		
		Decorator d = getNamedDecorator(request, layoutName);
		if(d != null) {
			return d;
		}
		else {
			return parent != null ? super.getDecorator(request, page) : null;
		}
	}

	public Decorator getNamedDecorator(HttpServletRequest request, String name) {
		if(StringUtils.isBlank(name))return null;
		
		if(decoratorMap.containsKey(name)) {
			return (Decorator)decoratorMap.get(name);
		}
		else {
			String decoratorName = name;
			if(!name.matches("(.+)(\\.)(\\w{2}|\\w{3})")) {
				name += DEFAULT_VIEW_TYPE;
			}
			String decoratorPage = DEFAULT_DECORATOR_PATH + '/' + name;

            ResourceLoader resourceLoader = establishResourceLoader();


            Resource res = resourceLoader.getResource(decoratorPage);
            if(!res.exists()) {

                PathMatchingResourcePatternResolver matcher = new PathMatchingResourcePatternResolver(resourceLoader);
                String pattern = GrailsResourceUtils.WEB_INF + "/plugins/*/grails-app/views/layouts/" + name;

                if(LOG.isDebugEnabled())
                    LOG.debug("No decorator found at " + decoratorPage+". Trying plug-ins with pattern: " + pattern);

                try {
                    Resource[] layouts = matcher.getResources(pattern);
                    if(layouts.length>0) {
                        if(layouts.length>1) {
                            LOG.warn("Multiple matching layouts found in plug-ins for name ["+name+"] using first from ["+ ArrayUtils.toString(layouts) +"]");
                        }
                        String url = layouts[0].getURL().toString();
                        url = GrailsResourceUtils.WEB_INF + url.substring(url.indexOf("/plugins"),url.length());
                        Decorator d = new DefaultDecorator(name, request.getRequestURI(), url, Collections.EMPTY_MAP);
                        decoratorMap.put(decoratorName, d);
                        return d;
                    }
                } catch (IOException e) {
                    // ignore, if there was a problem here its going to be a FNFE which is ok
                }
                catch (IllegalArgumentException e) {
                    // ignore, this is thrown when the plugins dir doesn't exist
                }
                return null;
            }
            else {
                if(LOG.isDebugEnabled())
                    LOG.debug("Using decorator " + decoratorPage);

                Decorator d = new DefaultDecorator(decoratorName,request.getRequestURI(),decoratorPage, Collections.EMPTY_MAP);
                decoratorMap.put(decoratorName,d);
                return d;
            }
		}
	}

    private ResourceLoader establishResourceLoader() {
        ResourceLoader resourceLoader;
        ApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        GrailsApplication application = null;
        if(ctx.containsBean(GrailsApplication.APPLICATION_ID)) {
            application = (GrailsApplication)ctx.getBean(GrailsApplication.APPLICATION_ID);
        }
        if(application == null) {
            resourceLoader = ctx;
        }
        else if(ctx.containsBean(GroovyPageResourceLoader.BEAN_ID) && !application.isWarDeployed()) {
            resourceLoader = (ResourceLoader)ctx.getBean(GroovyPageResourceLoader.BEAN_ID);
        }
        else {
            resourceLoader = ctx;
        }
        return resourceLoader;
    }

}
