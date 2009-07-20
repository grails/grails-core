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
package org.codehaus.groovy.grails.context.annotation;

import grails.util.BuildSettings;
import grails.util.BuildSettingsHolder;
import grails.util.Metadata;
import org.apache.commons.io.FilenameUtils;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerHolder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ComponentScanBeanDefinitionParser;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.AntPathMatcher;
import org.w3c.dom.Element;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Extends Spring's default &lt;context:component-scan/&gt; element to ignore Groovy's
 * generated closure classes
 *
 * @author Graeme Rocher
 * @since 1.2
 */
public class ClosureClassIgnoringComponentScanBeanDefinitionParser extends ComponentScanBeanDefinitionParser{

    @Override
    protected ClassPathBeanDefinitionScanner createScanner(XmlReaderContext readerContext, boolean useDefaultFilters) {
        final ClassPathBeanDefinitionScanner scanner = super.createScanner(readerContext, useDefaultFilters);
        GrailsPluginManager pluginManager = PluginManagerHolder.getPluginManager();
        if(pluginManager!=null) {
            List<TypeFilter> typeFilters = pluginManager.getTypeFilters();
            for (TypeFilter typeFilter : typeFilters) {
                scanner.addIncludeFilter(typeFilter);
            }
        }
        return scanner;
    }

    @Override
    protected ClassPathBeanDefinitionScanner configureScanner(ParserContext parserContext, Element element) {
        final ClassPathBeanDefinitionScanner scanner = super.configureScanner(parserContext, element);
        final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver(parserContext.getReaderContext().getResourceLoader()) {
            @Override
            protected Resource[] findAllClassPathResources(String location) throws IOException {
                Set<Resource> result = new LinkedHashSet<Resource>(16);


                URL classesDir = null;

                final boolean warDeployed = Metadata.getCurrent().isWarDeployed();
                if(!warDeployed) {
                    BuildSettings buildSettings = BuildSettingsHolder.getSettings();
                    if(buildSettings != null && buildSettings.getClassesDir()!=null) {
                        classesDir = buildSettings.getClassesDir().toURI().toURL();
                    }
                }

                // only scan classes from project classes directory
                String path = location;
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                Enumeration resourceUrls = getClassLoader().getResources(path);
                while (resourceUrls.hasMoreElements()) {
                    URL url = (URL) resourceUrls.nextElement();
                    if(!warDeployed && classesDir!= null && url.equals(classesDir)) {
                        result.add(convertClassLoaderURL(url));
                    }
                    else if(warDeployed){
                        result.add(convertClassLoaderURL(url));
                    }

                }                
                return result.toArray(new Resource[result.size()]);
            }
        };
        resourceResolver.setPathMatcher(new AntPathMatcher(){
            @Override
            public boolean match(String pattern, String path) {
                if(path.endsWith(".class")) {
                    String filename = FilenameUtils.getBaseName(path);
                    if(filename.indexOf("$")>-1) return false;
                }
                return super.match(pattern, path);    
            }
        });
        scanner.setResourceLoader(resourceResolver);
        return scanner;
    }
}
