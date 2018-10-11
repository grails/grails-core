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

import grails.plugins.GrailsPlugin
import grails.util.Environment
import groovy.transform.CompileStatic
import org.grails.config.NavigableMap
import org.grails.config.NavigableMapPropertySource
import org.springframework.beans.factory.config.YamlProcessor
import org.springframework.boot.env.PropertySourceLoader
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.env.PropertySource
import org.springframework.core.io.Resource

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
    List<PropertySource<?>> load(String name, Resource resource) throws IOException {
        return load(name, resource, Collections.<String>emptyList())
    }

    List<PropertySource<?>> load(String name, Resource resource, List<String> filteredKeys) throws IOException {
        List<Map<String, Object>> loaded = new OriginTrackedYamlLoader(resource).load()
        if (loaded.isEmpty()) {
            return Collections.emptyList()
        }
        List<PropertySource<?>> propertySources = new ArrayList<>(loaded.size())
        for (int i = 0; i < loaded.size(); i++) {
            String documentNumber = (loaded.size() != 1) ? " (document #" + i + ")" : ""
            def map = loaded.get(i)
            //Now merge the environment config over the top of the normal stuff
            def environments = map.get(GrailsPlugin.ENVIRONMENTS)
            String currentEnvironment = Environment.getCurrentEnvironment()?.name
            if (environments instanceof Map) {
                Map envMap = (Map) environments
                for (envSpecific in envMap) {
                    if (envSpecific instanceof Map || envSpecific instanceof Map.Entry) {
                        def environmentEntries = environments.get(envSpecific.key)
                        if (environmentEntries instanceof Map) {
                            if (envSpecific?.key?.toString()?.equalsIgnoreCase(currentEnvironment)) {
                                map.putAll(environmentEntries)
                            }
                        }
                    }
                }
            }

            for (String key in filteredKeys) {
                map.remove(key)
            }
            def propertySource = new NavigableMap()
            propertySource.merge(map, true)
            propertySources.add(
                    new NavigableMapPropertySource(name + documentNumber,propertySource))
        }

        return propertySources
    }

}
