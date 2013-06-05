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
package org.codehaus.groovy.grails.web.mapping;

import groovy.lang.Script;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.commons.GrailsUrlMappingsClass;
import org.codehaus.groovy.grails.commons.UrlMappingsArtefactHandler;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerAware;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * Constructs the UrlMappingsHolder from the registered UrlMappings class within a GrailsApplication.
 *
 * @author Graeme Rocher
 * @since 0.5
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class UrlMappingsHolderFactoryBean implements FactoryBean<UrlMappings>, InitializingBean, ApplicationContextAware, GrailsApplicationAware, PluginManagerAware {
    private static final String URL_MAPPING_CACHE_MAX_SIZE = "grails.urlmapping.cache.maxsize";
    private static final String URL_CREATOR_CACHE_MAX_SIZE = "grails.urlcreator.cache.maxsize";
    private GrailsApplication grailsApplication;
    private UrlMappings urlMappingsHolder;
    private GrailsPluginManager pluginManager;
    private ApplicationContext applicationContext;

    public UrlMappings getObject() throws Exception {
        return urlMappingsHolder;
    }

    public Class<UrlMappings> getObjectType() {
        return UrlMappings.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.state(applicationContext != null, "Property [applicationContext] must be set!");
        Assert.state(grailsApplication != null, "Property [grailsApplication] must be set!");

        List urlMappings = new ArrayList();
        List excludePatterns = new ArrayList();

        GrailsClass[] mappings = grailsApplication.getArtefacts(UrlMappingsArtefactHandler.TYPE);

        final DefaultUrlMappingEvaluator mappingEvaluator = new DefaultUrlMappingEvaluator((WebApplicationContext) applicationContext);
        mappingEvaluator.setPluginManager(pluginManager);

        if (mappings.length == 0) {
            urlMappings.addAll(mappingEvaluator.evaluateMappings(DefaultUrlMappings.getMappings()));
        }
        else {
            for (GrailsClass mapping : mappings) {
                GrailsUrlMappingsClass mappingClass = (GrailsUrlMappingsClass) mapping;
                List grailsClassMappings;
                if (Script.class.isAssignableFrom(mappingClass.getClazz())) {
                    grailsClassMappings = mappingEvaluator.evaluateMappings(mappingClass.getClazz());
                }
                else {
                    grailsClassMappings = mappingEvaluator.evaluateMappings(mappingClass.getMappingsClosure());
                }

                urlMappings.addAll(grailsClassMappings);
                if (mappingClass.getExcludePatterns() != null) {
                    excludePatterns.addAll(mappingClass.getExcludePatterns());
                }
            }
        }

        DefaultUrlMappingsHolder defaultUrlMappingsHolder = new DefaultUrlMappingsHolder(urlMappings, excludePatterns, true);

        Map flatConfig = grailsApplication.getFlatConfig();
        Integer cacheSize = mapGetInteger(flatConfig, URL_MAPPING_CACHE_MAX_SIZE);
        if (cacheSize != null) {
            defaultUrlMappingsHolder.setMaxWeightedCacheCapacity(cacheSize);
        }
        Integer urlCreatorCacheSize = mapGetInteger(flatConfig, URL_CREATOR_CACHE_MAX_SIZE);
        if (urlCreatorCacheSize != null) {
            defaultUrlMappingsHolder.setUrlCreatorMaxWeightedCacheCapacity(urlCreatorCacheSize);
        }
        // call initialize() after settings are in place
        defaultUrlMappingsHolder.initialize();
        urlMappingsHolder=defaultUrlMappingsHolder;
    }

    // this should possibly be somewhere in utility classes , MapUtils.getInteger doesn't handle GStrings/CharSequence
    private static Integer mapGetInteger(Map map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer)value;
        }
        return value instanceof Number ? ((Number)value).intValue() : Integer.valueOf(String.valueOf(value));
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    public void setServletContext(ServletContext servletContext) {
        // not used
    }

    public void setPluginManager(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    /**
     * Set the ApplicationContext that this object runs in.
     * Normally this call will be used to initialize the object.
     * <p>Invoked after population of normal bean properties but before an init callback such
     * as {@link org.springframework.beans.factory.InitializingBean#afterPropertiesSet()}
     * or a custom init-method. Invoked after {@link org.springframework.context.ResourceLoaderAware#setResourceLoader},
     * {@link org.springframework.context.ApplicationEventPublisherAware#setApplicationEventPublisher} and
     * {@link org.springframework.context.MessageSourceAware}, if applicable.
     *
     * @param applicationContext the ApplicationContext object to be used by this object
     * @throws org.springframework.context.ApplicationContextException
     *          in case of context initialization errors
     * @throws org.springframework.beans.BeansException
     *          if thrown by application context methods
     * @see org.springframework.beans.factory.BeanInitializationException
     */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        setGrailsApplication(applicationContext.getBean( GrailsApplication.APPLICATION_ID, GrailsApplication.class) );
        setServletContext(applicationContext instanceof WebApplicationContext ? ((WebApplicationContext) applicationContext).getServletContext() : null);
        setPluginManager( applicationContext.containsBean(GrailsPluginManager.BEAN_NAME) ? applicationContext.getBean(GrailsPluginManager.BEAN_NAME, GrailsPluginManager.class) : null);
    }
}
