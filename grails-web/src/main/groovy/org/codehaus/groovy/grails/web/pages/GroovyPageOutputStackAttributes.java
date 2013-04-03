package org.codehaus.groovy.grails.web.pages;

import java.io.Writer;

import org.codehaus.groovy.grails.support.encoding.Encoder;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;

public class GroovyPageOutputStackAttributes {
    private final Writer topWriter;
    private final Encoder templateEncoder;
    private final Encoder outEncoder;
    private final Encoder expressionEncoder;
    private final boolean allowCreate;
    private final boolean pushTop;
    private final boolean autoSync;
    private final boolean inheritPreviousEncoders;
    private final GrailsWebRequest webRequest;

    public boolean isInheritPreviousEncoders() {
        return inheritPreviousEncoders;
    }

    public Writer getTopWriter() {
        return topWriter;
    }
    
    public Encoder getTemplateEncoder() {
        return templateEncoder;
    }

    public Encoder getOutEncoder() {
        return outEncoder;
    }

    public Encoder getExpressionEncoder() {
        return expressionEncoder;
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
        private Encoder outEncoder;
        private Encoder expressionEncoder;
        private boolean allowCreate=true;
        private boolean pushTop=true;
        private boolean autoSync=true;
        private GrailsWebRequest webRequest;
        private boolean inheritPreviousEncoders=false;
        
        public Builder() {
            
        }
        
        public Builder(GroovyPageOutputStackAttributes attributes) {
            this.topWriter = attributes.topWriter;
            this.templateEncoder = attributes.templateEncoder;
            this.outEncoder = attributes.outEncoder;
            this.expressionEncoder = attributes.expressionEncoder;
            this.allowCreate = attributes.allowCreate;
            this.pushTop = attributes.pushTop;
            this.autoSync = attributes.autoSync;
            this.webRequest = attributes.webRequest;
            this.inheritPreviousEncoders = attributes.inheritPreviousEncoders;
        }

        public Builder topWriter(Writer topWriter) {
            this.topWriter = topWriter;
            return this;
        }

        public Builder templateEncoder(Encoder templateEncoder) {
            this.templateEncoder = templateEncoder;
            return this;
        }

        public Builder outEncoder(Encoder outEncoder) {
            this.outEncoder = outEncoder;
            return this;
        }

        public Builder expressionEncoder(Encoder expressionEncoder) {
            this.expressionEncoder = expressionEncoder;
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

        public Builder inheritPreviousEncoders(boolean inheritPreviousEncoders) {
            this.inheritPreviousEncoders = inheritPreviousEncoders;
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
        this.outEncoder = builder.outEncoder;
        this.expressionEncoder = builder.expressionEncoder;
        this.allowCreate = builder.allowCreate;
        this.pushTop = builder.pushTop;
        this.autoSync = builder.autoSync;
        this.webRequest = builder.webRequest;
        this.inheritPreviousEncoders = builder.inheritPreviousEncoders;
    }
}
