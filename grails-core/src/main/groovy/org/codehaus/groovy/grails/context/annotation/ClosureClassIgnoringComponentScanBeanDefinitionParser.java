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

import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerHolder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.annotation.ComponentScanBeanDefinitionParser;
import org.springframework.context.annotation.ComponentScanSpec;
import org.springframework.context.config.FeatureSpecification;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.w3c.dom.Element;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Extends Spring's default &lt;context:component-scan/&gt; element to ignore
 * generated classes.
 *
 * @author Graeme Rocher
 * @author Lari Hotari
 * @since 1.2
 */
public class ClosureClassIgnoringComponentScanBeanDefinitionParser extends ComponentScanBeanDefinitionParser{


    private static final String DOLLAR_CONTAINING_PATTERN = ".+\\\\$.+";

    @Override
    public FeatureSpecification doParse(Element element, ParserContext parserContext) {
        ComponentScanSpec spec = (ComponentScanSpec) super.doParse(element, parserContext);
        GrailsPluginManager pluginManager = PluginManagerHolder.getPluginManager();
        if (pluginManager != null) {
            List<TypeFilter> typeFilters = pluginManager.getTypeFilters();
            spec.includeFilters(typeFilters.toArray(new TypeFilter[typeFilters.size()]));

        }

        spec.excludeFilters(new RegexPatternTypeFilter(Pattern.compile(DOLLAR_CONTAINING_PATTERN)));

        return spec;
    }
}
