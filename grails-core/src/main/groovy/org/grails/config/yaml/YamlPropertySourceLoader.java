/*
 * Copyright 2018 original authors
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

package org.grails.config.yaml;


import grails.plugins.GrailsPlugin;
import grails.util.Environment;
import org.grails.config.NavigableMap;
import org.grails.config.NavigableMapPropertySource;
import org.springframework.beans.factory.config.YamlProcessor;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Replacement for Spring Boot's YAML loader that uses Grails' NavigableMap.
 *
 * @author graemerocher
 * @since 3.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class YamlPropertySourceLoader extends YamlProcessor implements PropertySourceLoader {
    @Override
    public String[] getFileExtensions() {
        return new String[] { "yml", "yaml" };
    }

    @Override
    public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
        return load(name, resource, Collections.emptyList());
    }

    public List<PropertySource<?>> load(String name, Resource resource, List<String> filteredKeys) throws IOException {
        setResources(resource);
        List<Map<String, Object>> loaded = load();
        if (loaded.isEmpty()) {
            return Collections.emptyList();
        }
        List<PropertySource<?>> propertySources = new ArrayList<>(loaded.size());
        for (int i = 0; i < loaded.size(); i++) {
            String documentNumber = (loaded.size() != 1) ? " (document #" + i + ")" : "";
            final Map<String, Object> map = loaded.get(i);
            //Now merge the environment config over the top of the normal stuff
            final Object environments = map.get(GrailsPlugin.ENVIRONMENTS);
            final Environment env = Environment.getCurrentEnvironment();
            String currentEnvironment = env != null ? env.getName() : null;
            if (environments instanceof Map) {
                Map envMap = (Map) environments;
                for (Object envSpecific : envMap.entrySet()) {
                    if (envSpecific instanceof Map.Entry) {
                        final Map.Entry envSpecificEntry = (Map.Entry) envSpecific;
                        Object environmentEntries = envMap.get(envSpecificEntry.getKey());
                        if (environmentEntries instanceof Map) {
                            if (envSpecificEntry.getKey().toString().equalsIgnoreCase(currentEnvironment)) {
                                map.putAll((Map<? extends String, ?>) environmentEntries);
                            }
                        }
                    }
                }
            }

            for (String filteredKey : filteredKeys) {
                map.remove(filteredKey);
            }
            NavigableMap propertySource = new NavigableMap();
            propertySource.merge(map, true);
            propertySources.add(
                    new NavigableMapPropertySource(name + documentNumber,propertySource));
        }

        return propertySources;
    }


    public List<Map<String, Object>> load() {
        final List<Map<String, Object>> result = new ArrayList<>();
        process((properties, map) -> result.add(getFlattenedMap(map)));
        return result;
    }
}
