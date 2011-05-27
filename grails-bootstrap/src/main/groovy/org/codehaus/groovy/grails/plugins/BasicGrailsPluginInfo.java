/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.plugins;

import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.Resource;

/**
 * Simple Javabean implementation of the GrailsPluginInfo interface.
 *
 * @author Graeme Rocher
 * @since 1.3
 */
public class BasicGrailsPluginInfo extends GroovyObjectSupport implements GrailsPluginInfo {

    private String name;
    private String version;
    private Map<String,Object> attributes = new ConcurrentHashMap<String,Object>();
    private Resource descriptor;

    public BasicGrailsPluginInfo(Resource pluginLocation) {
        descriptor = pluginLocation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public void setProperty(String property, Object newValue) {
        try {
            super.setProperty(property, newValue);
        }
        catch (MissingPropertyException e) {
            attributes.put(property, newValue);
        }
    }

    @Override
    public Object getProperty(String property) {
        try {
            return super.getProperty(property);
        }
        catch (MissingPropertyException e) {
            return attributes.get(property);
        }
    }

    public String getFullName() {
        return name + '-' + version;
    }

    public Resource getDescriptor() {
        return descriptor;
    }

    public Resource getPluginDir() {
        try {
            return descriptor.createRelative(".");
        }
        catch (IOException e) {
            return null;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Map getProperties() {
        Map props = new HashMap();
        props.putAll(attributes);
        props.put(NAME, name);
        props.put(VERSION, version);
        return props;
    }
}
