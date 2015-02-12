/* Copyright 2014 the original author or authors.
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
package org.grails.core;

import grails.config.Config;
import grails.core.ArtefactHandler;
import grails.core.GrailsApplication;
import grails.core.support.GrailsConfigurationAware;
import grails.util.Holders;
import grails.util.Metadata;
import groovy.lang.GroovyObjectSupport;
import groovy.util.ConfigObject;
import org.grails.config.PropertySourcesConfig;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.*;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.ClassUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractGrailsApplication extends GroovyObjectSupport implements GrailsApplication, ApplicationContextAware, BeanClassLoaderAware, ApplicationListener<ApplicationEvent> {
    protected ClassLoader classLoader;
    protected Config config;
    @SuppressWarnings("rawtypes")
    protected Map flatConfig = Collections.emptyMap();
    protected ApplicationContext parentContext;
    protected Metadata applicationMeta = Metadata.getCurrent();
    protected boolean contextInitialized;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.parentContext = applicationContext;
        if(applicationContext instanceof ConfigurableApplicationContext) {
            ((ConfigurableApplicationContext)applicationContext).addApplicationListener(this);
        }
    }

    @Override
    public Metadata getMetadata() {
        return applicationMeta;
    }

    @Override
    public boolean isWarDeployed() {
        return getMetadata().isWarDeployed();
    }
    
    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
        Holders.setConfig(config);
        updateFlatConfig();
    }

    public void setConfig(ConfigObject config) {
        this.config = new PropertySourcesConfig().merge(config);
        Holders.setConfig(this.config);
        updateFlatConfig();
    }

    @SuppressWarnings("rawtypes")
    public void updateFlatConfig() {
        if (config == null) {
            flatConfig = new LinkedHashMap();
        } else {
            flatConfig = config.flatten();
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getFlatConfig() {
        return getConfig().flatten();
    }

    @Override
    public void configChanged() {
        updateFlatConfig();
        final ArtefactHandler[] handlers = getArtefactHandlers();
        if(handlers != null) {
            for (ArtefactHandler handler : handlers) {
                if (handler instanceof GrailsConfigurationAware) {
                    ((GrailsConfigurationAware)handler).setConfiguration(config);
                }
            }
        }
    }
    
    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
    
    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class getClassForName(String className) {
        return ClassUtils.resolveClassName(className, getClassLoader());
    }    
    
    public ApplicationContext getMainContext() {
        return parentContext;
    }

    public void setMainContext(ApplicationContext context) {
        this.parentContext = context;
    }

    public ApplicationContext getParentContext() {
        return parentContext;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if(event instanceof ContextRefreshedEvent) {
            this.contextInitialized = true;
        }
    }
}
