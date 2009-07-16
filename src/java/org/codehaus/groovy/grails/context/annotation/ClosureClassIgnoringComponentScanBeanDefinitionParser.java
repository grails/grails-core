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

import org.springframework.context.annotation.ComponentScanBeanDefinitionParser;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.AntPathMatcher;
import org.w3c.dom.Element;
import org.apache.commons.io.FilenameUtils;

/**
 * Extends Spring's default &lt;context:component-scan/&gt; element to ignore Groovy's
 * generated closure classes
 *
 * @author Graeme Rocher
 * @since 1.2
 */
public class ClosureClassIgnoringComponentScanBeanDefinitionParser extends ComponentScanBeanDefinitionParser{

    @Override
    protected ClassPathBeanDefinitionScanner configureScanner(ParserContext parserContext, Element element) {
        final ClassPathBeanDefinitionScanner scanner = super.configureScanner(parserContext, element);
        final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver(parserContext.getReaderContext().getResourceLoader());
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

    class DelegatingResourceLoader implements ResourceLoader{
        private ResourceLoader delegate;

        DelegatingResourceLoader(ResourceLoader loader) {
            this.delegate = loader;
        }

        public Resource getResource(String location) {
            System.out.println("location = " + location);
            return delegate.getResource(location);
        }

        public ClassLoader getClassLoader() {
            return delegate.getClassLoader();
        }
    }
}
