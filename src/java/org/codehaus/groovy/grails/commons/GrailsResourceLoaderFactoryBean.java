/* Copyright 2006-2007 Graeme Rocher
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
package org.codehaus.groovy.grails.commons;

import grails.util.Environment;

import java.io.IOException;

import org.codehaus.groovy.grails.commons.spring.GrailsResourceHolder;
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoader;
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoaderHolder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;

/**
 * A factory bean that constructs the Grails ResourceLoader used to load Grails classes.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
public class GrailsResourceLoaderFactoryBean implements FactoryBean<GrailsResourceLoader>, InitializingBean {

    private GrailsResourceHolder grailsResourceHolder;
    private GrailsResourceLoader resourceLoader;

    public void setGrailsResourceHolder(GrailsResourceHolder grailsResourceHolder) {
        this.grailsResourceHolder = grailsResourceHolder;
    }

    public GrailsResourceLoader getObject() {
        return resourceLoader;
    }

    public Class<GrailsResourceLoader> getObjectType() {
        return GrailsResourceLoader.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(grailsResourceHolder, "Property [grailsResourceHolder] must be set!");

        resourceLoader = GrailsResourceLoaderHolder.getResourceLoader();
        if (resourceLoader == null) {
            if (Environment.getCurrent().isReloadEnabled()) {
                ResourcePatternResolver resolver = new  PathMatchingResourcePatternResolver();
                try {
                    Resource[] resources = resolver.getResources("file:" +
                            Environment.getCurrent().getReloadLocation() + "/grails-app/**/*.groovy");
                    resourceLoader = new GrailsResourceLoader(resources);
                }
                catch (IOException e) {
                    createDefaultInternal();
                }
            }
            else {
                createDefaultInternal();
            }
            GrailsResourceLoaderHolder.setResourceLoader(resourceLoader);
        }
    }

    private void createDefaultInternal() {
        resourceLoader = new GrailsResourceLoader(grailsResourceHolder.getResources());
    }
}
