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
package org.codehaus.groovy.grails.web.mapping;

import groovy.lang.Script;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.commons.GrailsUrlMappingsClass;
import org.codehaus.groovy.grails.commons.UrlMappingsArtefactHandler;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerAware;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Constructs the UrlMappingsHolder from the registered UrlMappings class within a GrailsApplication.
 *
 * @author Graeme Rocher
 * @since 0.5
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class UrlMappingsHolderFactoryBean implements FactoryBean<UrlMappingsHolder>, InitializingBean, GrailsApplicationAware, ServletContextAware, PluginManagerAware {
    private static final String URL_MAPPING_CACHE_MAX_SIZE = "grails.urlmapping.cache.maxsize";
    private static final String URL_CREATOR_CACHE_MAX_SIZE = "grails.urlcreator.cache.maxsize";
    private GrailsApplication grailsApplication;
    private UrlMappingsHolder urlMappingsHolder;
    private UrlMappingEvaluator mappingEvaluator;
    private ServletContext servletContext;
    private GrailsPluginManager pluginManager;

    public UrlMappingsHolder getObject() throws Exception {
        return urlMappingsHolder;
    }

    public Class<UrlMappingsHolder> getObjectType() {
        return UrlMappingsHolder.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.state(grailsApplication != null, "Property [grailsApplication] must be set!");

        List urlMappings = new ArrayList();
        List excludePatterns = new ArrayList();

        GrailsClass[] mappings = grailsApplication.getArtefacts(UrlMappingsArtefactHandler.TYPE);

        final DefaultUrlMappingEvaluator defaultUrlMappingEvaluator = new DefaultUrlMappingEvaluator(servletContext);
        defaultUrlMappingEvaluator.setPluginManager(pluginManager);
        mappingEvaluator = defaultUrlMappingEvaluator;

        if (mappings.length == 0) {
            urlMappings.addAll(mappingEvaluator.evaluateMappings(DefaultUrlMappings.getMappings()));
        }
        else {
            for (GrailsClass mapping : mappings) {
                GrailsUrlMappingsClass mappingClass = (GrailsUrlMappingsClass) mapping;
                List grailsClassMappings;
                if (Script.class.isAssignableFrom(mappingClass.getClass())) {
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
        Object value=map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer)value;
        }
        return (value instanceof Number) ? Integer.valueOf(((Number)value).intValue()) : Integer.valueOf(String.valueOf(value));
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void setPluginManager(GrailsPluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }


}
