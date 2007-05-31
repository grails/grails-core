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
package org.codehaus.groovy.grails.web.servlet.filter;

import groovy.lang.GroovyClassLoader;
import groovy.lang.Writable;
import groovy.text.Template;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerHolder;
import org.codehaus.groovy.grails.web.errors.GrailsWrappedRuntimeException;
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine;
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A servlet filter that copies resources from the source on content change and manages reloading if necessary
 *
 * @author Graeme Rocher
 * @since Jan 10, 2006
 */
public class GrailsReloadServletFilter extends OncePerRequestFilter {

    public static final Log LOG = LogFactory.getLog(GrailsReloadServletFilter.class);

    ResourceCopier copyScript;
    GrailsWebApplicationContext context;
    GrailsApplication application;

    private GrailsRuntimeConfigurator config;

	private GrailsPluginManager manager;
    private UrlPathHelper urlHelper = new UrlPathHelper();
    private WebApplicationContext parent;

    public GrailsReloadServletFilter() {
    }


    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {


      GrailsApplicationAttributes attrs = new DefaultGrailsApplicationAttributes(getServletContext());
      context = (GrailsWebApplicationContext)attrs.getApplicationContext();

      if(LOG.isDebugEnabled()) {
	      LOG.debug("Executing Grails reload filter...");
      }
      if(context == null) {
          filterChain.doFilter(httpServletRequest,httpServletResponse);
          return;
      }
      application = (GrailsApplication)context.getBean(GrailsApplication.APPLICATION_ID);
      if(application == null) {
          filterChain.doFilter(httpServletRequest,httpServletResponse);
          return;
      }      
      if(config == null) {
    	  this.parent = (WebApplicationContext)getServletContext().getAttribute(GrailsApplicationAttributes.PARENT_APPLICATION_CONTEXT);
    	  config = new GrailsRuntimeConfigurator(application,parent);  
      }


      String uri = urlHelper.getPathWithinApplication(httpServletRequest);
      String lastPart = uri.substring(uri.lastIndexOf("/"));
      if(lastPart.indexOf('.') > -1) {
          filterChain.doFilter(httpServletRequest, httpServletResponse);
          return;
      }

      if(copyScript == null) {
          GroovyClassLoader gcl = new GroovyClassLoader(getClass().getClassLoader());

          Class groovyClass;
          try {
              groovyClass = gcl.parseClass(gcl.getResource("org/codehaus/groovy/grails/web/servlet/filter/GrailsResourceCopier.groovy").openStream());
              copyScript = (ResourceCopier)groovyClass.newInstance();
          } catch (IllegalAccessException e) {
              LOG.error("Illegal access creating resource copier. Save/reload disabled: " + e.getMessage(), e);
          } catch (InstantiationException e) {
              LOG.error("Error instantiating resource copier. Save/reload disabled: " + e.getMessage(), e);
          } catch (CompilationFailedException e) {
               LOG.error("Error compiling resource copier. Save/reload disabled: " + e.getMessage(), e);
          } catch(Exception e) {
             LOG.error("Error loading resource copier. Save/reload disabled: " + e.getMessage(), e);
          }
        }
	if(LOG.isDebugEnabled()) {
	      LOG.debug("Running copy script...");
	}	
        if(copyScript != null) {
            copyScript.copyResourceBundles();
        } 

        
        if(manager == null) {
        	manager = PluginManagerHolder.getPluginManager();
        }
        try {
            if(manager != null) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Checking Plugin manager for changes..");
                manager.checkForChanges();
                if(!application.isInitialised()) {
                    application.rebuild();
                    config = new GrailsRuntimeConfigurator(application,parent);
                    config.reconfigure(context, getServletContext(), true);
                }
                
            }
            else if(LOG.isDebugEnabled()) {
                LOG.debug("Plugin manager not found, skipping change check");
            }
        } catch (Exception e) {
            LOG.error("Error occured reloading application: " + e.getMessage(),e);

            httpServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            GroovyPagesTemplateEngine engine = attrs.getPagesTemplateEngine();
            // TODO GRAILS-1190 we have no idea here what layout the returned page would have been going to use had the exception
            //          not happened....
            Template t = engine.createTemplate(GrailsApplicationAttributes.PATH_TO_VIEWS + "/error.gsp");

            GrailsWrappedRuntimeException wrapped = new GrailsWrappedRuntimeException(getServletContext(), e);
            Map model = new HashMap();
            model.put("exception", wrapped);

            Writable w = t.make(model);

            w.writeTo(httpServletResponse.getWriter());
            
        }
        filterChain.doFilter(httpServletRequest,httpServletResponse);
    }

}
