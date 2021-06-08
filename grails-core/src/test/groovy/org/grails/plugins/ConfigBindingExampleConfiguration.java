package org.grails.plugins;

import org.springframework.context.annotation.Configuration;

@Configuration
class ConfigBindingExampleConfiguration {

    private final ConfigBindingExampleProperties configBindingExampleProperties;

    ConfigBindingExampleConfiguration(ConfigBindingExampleProperties configBindingExampleProperties) {
        this.configBindingExampleProperties = configBindingExampleProperties;
    }
}
