package org.codehaus.groovy.grails.web.pages;

import java.io.Writer;

import org.codehaus.groovy.grails.support.encoding.Encoder;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;

public class GroovyPageOutputStackAttributes {
    private Writer topWriter;
    private final Encoder templateEncoder;
    private final Encoder pageEncoder;
    private final Encoder defaultEncoder;
    private final boolean allowCreate;
    private final boolean pushTop;
    private final boolean autoSync;
    private final GrailsWebRequest webRequest;

    public Writer getTopWriter() {
        return topWriter;
    }
    
    public void setTopWriter(Writer topWriter) {
        this.topWriter = topWriter;
    }

    public Encoder getTemplateEncoder() {
        return templateEncoder;
    }

    public Encoder getPageEncoder() {
        return pageEncoder;
    }

    public Encoder getDefaultEncoder() {
        return defaultEncoder;
    }

    public boolean isAllowCreate() {
        return allowCreate;
    }

    public boolean isPushTop() {
        return pushTop;
    }

    public boolean isAutoSync() {
        return autoSync;
    }

    public GrailsWebRequest getWebRequest() {
        return webRequest;
    }

    public static class Builder {
        private Writer topWriter;
        private Encoder templateEncoder;
        private Encoder pageEncoder;
        private Encoder defaultEncoder;
        private boolean allowCreate;
        private boolean pushTop;
        private boolean autoSync;
        private GrailsWebRequest webRequest;

        public Builder topWriter(Writer topWriter) {
            this.topWriter = topWriter;
            return this;
        }

        public Builder templateEncoder(Encoder templateEncoder) {
            this.templateEncoder = templateEncoder;
            return this;
        }

        public Builder pageEncoder(Encoder pageEncoder) {
            this.pageEncoder = pageEncoder;
            return this;
        }

        public Builder defaultEncoder(Encoder defaultEncoder) {
            this.defaultEncoder = defaultEncoder;
            return this;
        }

        public Builder allowCreate(boolean allowCreate) {
            this.allowCreate = allowCreate;
            return this;
        }

        public Builder pushTop(boolean pushTop) {
            this.pushTop = pushTop;
            return this;
        }

        public Builder autoSync(boolean autoSync) {
            this.autoSync = autoSync;
            return this;
        }

        public Builder webRequest(GrailsWebRequest webRequest) {
            this.webRequest = webRequest;
            return this;
        }

        public GroovyPageOutputStackAttributes build() {
            return new GroovyPageOutputStackAttributes(this);
        }
    }

    private GroovyPageOutputStackAttributes(Builder builder) {
        this.topWriter = builder.topWriter;
        this.templateEncoder = builder.templateEncoder;
        this.pageEncoder = builder.pageEncoder;
        this.defaultEncoder = builder.defaultEncoder;
        this.allowCreate = builder.allowCreate;
        this.pushTop = builder.pushTop;
        this.autoSync = builder.autoSync;
        this.webRequest = builder.webRequest;
    }
}
