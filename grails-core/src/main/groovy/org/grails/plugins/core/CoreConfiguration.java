/*
 * Copyright 2024 original authors
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
package org.grails.plugins.core;

import grails.config.ConfigProperties;
import grails.core.GrailsApplication;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.ShutdownEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Core beans.
 *
 * @author graemerocher
 * @since 4.0
 */
@Factory
public class CoreConfiguration implements ApplicationEventListener<ShutdownEvent> {

    private final GrailsApplication grailsApplication;
    private ConfigurableApplicationContext childContext;

    public CoreConfiguration(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    @Bean("classLoader")
    @Primary
    ClassLoader classLoader() {
        return grailsApplication.getClassLoader();
    }

    @Bean("grailsConfigProperties")
    @Primary
    ConfigProperties configProperties() {
        return new ConfigProperties(grailsApplication.getConfig());
    }

    /**
     * Sets the child Spring context.
     * @param childContext The child context
     */
    public void setChildContext(ConfigurableApplicationContext childContext) {
        this.childContext = childContext;
    }

    public ConfigurableApplicationContext getChildContext() {
        return childContext;
    }

    @Override
    public void onApplicationEvent(ShutdownEvent event) {
        if (childContext != null) {
            childContext.close();
        }
    }
}
