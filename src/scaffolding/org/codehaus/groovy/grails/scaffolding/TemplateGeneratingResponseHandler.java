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
package org.codehaus.groovy.grails.scaffolding;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.scaffolding.exceptions.ScaffoldingException;
import org.codehaus.groovy.grails.web.pages.GroovyPage;
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * A response handler that uses a GroovyPageTemplateEngine instance to write Templates directly to the response.
 * Also requires a reference to the ViewResolver to establish whether a view already exists for the response, in
 * which case it delegates to the view.
 *
 * @see org.springframework.web.servlet.ViewResolver
 * @see org.codehaus.groovy.grails.web.servlet.view.GrailsViewResolver
 * @see org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
 *
 * @author Graeme Rocher
 * @since 0.5
 *
 *        <p/>
 *        Created: Feb 27, 2007
 *        Time: 7:45:46 AM
 */
public class TemplateGeneratingResponseHandler implements ScaffoldResponseHandler, ApplicationContextAware {
    private ViewResolver resolver;
    private GrailsTemplateGenerator templateGenerator;
    private Map generatedViewCache = new HashMap();
    private Class scaffoldedClass;
    private GrailsApplication grailsApplication;
    
    private static final Log LOG = LogFactory.getLog(TemplateGeneratingResponseHandler.class);
    private ApplicationContext applicationContext;

    /**
     * Clears the cache of generated views. Scaffolded views will subsequently be re-generated
     */
    public void clearViewCache() {
        generatedViewCache.clear();
        getTemplateEngine().clearPageCache();
    }

    private GroovyPagesTemplateEngine getTemplateEngine() {
        GroovyPagesTemplateEngine templateEngine = (GroovyPagesTemplateEngine)applicationContext.getBean(GroovyPagesTemplateEngine.BEAN_ID);
        if(this.grailsApplication.isWarDeployed()) {
            templateEngine.setResourceLoader(this.applicationContext);
        }
        return templateEngine;
    }

    /**
     * Either delegates to a pre-existing view or renders an in-memory scaffolded view to the response
     *
     * @param request The HttpServletRequest
     * @param response The HttpServletResponse
     * @param actionName The name of the action to handle
     * @param model The View model
     * @return A Spring ModelAndView instance
     */
    public ModelAndView handleResponse(HttpServletRequest request, HttpServletResponse response, String actionName, Map model) {

        if(templateGenerator == null) throw new IllegalStateException("Property [templateGenerator] must be set!");
        if(resolver == null) throw new IllegalStateException("Property [resolver] must be set!");
        if(scaffoldedClass == null) throw new IllegalStateException("Property [scaffoldedClass] must be set!");
        if(grailsApplication == null) throw new IllegalStateException("Property [grailsApplication] must be set!");


        GrailsDomainClass domainClass = (GrailsDomainClass)grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,scaffoldedClass.getName());
        GrailsControllerClass controllerClass = grailsApplication.getScaffoldingController(domainClass);
        String uri = controllerClass.getViewByName(actionName);

        if(LOG.isDebugEnabled()) {
            LOG.debug("Scaffolding response for view URI: " + uri);
        }
        
        try {
            // if the view exists physically then fall back to delegating to the physical view
            View v = resolver.resolveViewName(uri, RequestContextUtils.getLocale(request));
            if(v instanceof AbstractUrlBasedView) {
                
                uri = ((AbstractUrlBasedView)v).getUrl();
                Resource r = null;
                if(uri.endsWith(GroovyPage.EXTENSION)) {
                    GroovyPagesTemplateEngine templateEngine = getTemplateEngine();
                    r = templateEngine.getResourceForUri(uri);
                }
                
                if(r != null && r.exists()) {
                    return new ModelAndView(v, model);
                }
                else {
                    return createScaffoldedResponse(uri, model, actionName);
                }
            }
            else {
                return createScaffoldedResponse(uri, model, actionName);
            }

        } catch (Exception e) {
            throw new ScaffoldingException("Unable to render scaffolded view for uri ["+uri+"]:" + e.getMessage(),e);
        }
    }

    /**
     * Takes the given URI and model and either retrieves a cached view or generates and places the generated view
     * into a ScaffoldedGroovyPageView instance
     *
     * @param uri The URI of the view
     * @param model The model of the view
     * @param actionName
     * @return A Spring ModelAndView instance
     */
    protected ModelAndView createScaffoldedResponse(String uri, Map model, String actionName) {
        View v;
        if(generatedViewCache.containsKey(uri)) {
            v = (View)generatedViewCache.get(uri);
        }
        else {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);

            if(grailsApplication == null) throw new IllegalStateException("Property [grailsApplication] must be set!");

            if(LOG.isDebugEnabled()) {
                LOG.debug("Generating view ["+uri+"] for scaffolded class ["+scaffoldedClass+"]");
            }
            if(grailsApplication.isWarDeployed()) {
                templateGenerator.setResourceLoader(this.applicationContext);
            }
            templateGenerator.generateView((GrailsDomainClass)grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
                                                                scaffoldedClass.getName()),
                                                                actionName,
                                                                pw);

            ScaffoldedGroovyPageView scaffoldedView = new ScaffoldedGroovyPageView(uri,sw.toString());
            
            scaffoldedView.setApplicationContext(applicationContext);
            
            v = scaffoldedView;
            generatedViewCache.put(uri, v);
        }
        return new ModelAndView(v,model);
    }

    /**
     * Sets the ViewResolver this ResponseHandler should use to resolve pre-existing views
     *
     * @param resolver The ViewResolver
     */
    public void setViewResolver(ViewResolver resolver) {
        this.resolver = resolver;
    }
    /**
     * Sets the Scaffolding GrailsTemplateGenerator to use when generating templates
     *
     * @param templateGenerator The GrailsTemplateGenerator instance
     */
    public void setTemplateGenerator(GrailsTemplateGenerator templateGenerator) {
        this.templateGenerator = templateGenerator;
    }


    public void setScaffoldedClass(Class scaffoldedClass) {
        this.scaffoldedClass = scaffoldedClass;
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
