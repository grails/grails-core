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
package org.codehaus.groovy.grails.web.pages;

import groovy.lang.Binding;

import java.util.Map;

public class GroovyPageBinding extends Binding {

    private String pluginContextPath;

    public GroovyPageBinding() {
        super();
    }

    @SuppressWarnings("rawtypes")
    public GroovyPageBinding(Map variables) {
        super(variables);
    }

    public GroovyPageBinding(String[] args) {
        super(args);
    }

    public GroovyPageBinding(String pluginContextPath) {
        this.pluginContextPath = pluginContextPath;
    }

    public String getPluginContextPath() {
        return pluginContextPath;
    }

    public void setPluginContextPath(String pluginContextPath) {
        this.pluginContextPath = pluginContextPath;
    }

    @Override
    public Object getProperty(String property) {
        if (getMetaClass().hasProperty(this, property) != null) {
            return getMetaClass().getProperty(this, property);
        }
        return getVariable(property);
    }

    @Override
    public void setProperty(String property, Object newValue) {
        setVariable(property, newValue);
    }
}