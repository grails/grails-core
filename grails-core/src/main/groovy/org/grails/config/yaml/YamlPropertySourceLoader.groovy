/*
 * Copyright 2014 original authors
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
package org.grails.config.yaml

import grails.util.Metadata
import groovy.transform.CompileStatic
import org.grails.config.NavigableMap
import org.grails.config.NavigableMapPropertySource
import org.grails.core.cfg.GroovyConfigPropertySourceLoader
import org.springframework.beans.factory.config.YamlProcessor
import org.springframework.boot.env.PropertySourceLoader
import org.springframework.boot.yaml.SpringProfileDocumentMatcher
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.core.io.Resource
import org.springframework.util.ClassUtils

/**
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
@Order(Ordered.HIGHEST_PRECEDENCE)
class YamlPropertySourceLoader extends YamlProcessor implements PropertySourceLoader {
    @Override
    String[] getFileExtensions() {
        ['yml', 'yaml'] as String[]
    }

    @Override
    PropertySource<?> load(String name, Resource resource, String profile) throws IOException {
        return load(name, resource, profile, true)
    }

    PropertySource<?> load(String name, Resource resource, String profile, boolean parseFlatKeys ) throws IOException {
        if (ClassUtils.isPresent("org.yaml.snakeyaml.Yaml", null)) {
            boolean matchDefault
            if (profile == null) {
                matchDefault = true
                setMatchDefault(matchDefault)
                setDocumentMatchers(new SpringProfileDocumentMatcher())
            }
            else {
                matchDefault = false;
                setMatchDefault(matchDefault)
                setDocumentMatchers(new SpringProfileDocumentMatcher(profile))
            }
            resources = [resource] as Resource[]
            def propertySource = new NavigableMap()
            def metadata = Metadata.getCurrent()
            def metadataSource = metadata.getSource()
            def metadataFile = metadata.getMetadataFile()
            if(matchDefault && metadataSource != null && metadataFile != null && metadataFile.getURL().equals(resource.getURL())) {
                for(o in metadataSource) {
                    if(o instanceof Map) {
                        propertySource.merge((Map)o, false)
                    }
                }
            }
            else {
                process { Properties properties, Map<String, Object> map ->
                    propertySource.merge(map, parseFlatKeys)
                }
            }

            if (!propertySource.isEmpty()) {
                return new NavigableMapPropertySource(name, propertySource)
            }
        }
        return null
    }


}
