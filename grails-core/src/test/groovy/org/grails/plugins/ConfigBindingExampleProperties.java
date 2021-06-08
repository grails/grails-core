package org.grails.plugins;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("example")
class ConfigBindingExampleProperties {

    private String bar = "default";

    public String getBar() {
        return bar;
    }

    public void setBar(String bar) {
        this.bar = bar;
    }
}
