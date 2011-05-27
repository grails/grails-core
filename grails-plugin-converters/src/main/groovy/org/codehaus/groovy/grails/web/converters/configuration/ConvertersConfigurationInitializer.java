/*
 * Copyright 2004-2008 the original author or authors.
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
package org.codehaus.groovy.grails.web.converters.configuration;

import grails.converters.JSON;
import grails.converters.XML;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.cfg.GrailsConfig;
import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler;
import org.codehaus.groovy.grails.support.proxy.ProxyHandler;
import org.codehaus.groovy.grails.web.converters.Converter;
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller;
import org.codehaus.groovy.grails.web.converters.marshaller.ProxyUnwrappingMarshaller;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Siegfried Puchbauer
 * @since 1.1
 */
public class ConvertersConfigurationInitializer implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public final Log LOG = LogFactory.getLog(ConvertersConfigurationInitializer.class);

    public void initialize(GrailsApplication application) {
        LOG.debug("Initializing Converters Default Configurations...");
        initJSONConfiguration(application);
        initXMLConfiguration(application);
        initDeepJSONConfiguration(application);
        initDeepXMLConfiguration(application);
    }

    private void initJSONConfiguration(GrailsApplication application) {
        LOG.debug("Initializing default JSON Converters Configuration...");

        List<ObjectMarshaller<JSON>> marshallers = new ArrayList<ObjectMarshaller<JSON>>();
        marshallers.add(new org.codehaus.groovy.grails.web.converters.marshaller.json.ArrayMarshaller());
        marshallers.add(new org.codehaus.groovy.grails.web.converters.marshaller.json.ByteArrayMarshaller());
        marshallers.add(new org.codehaus.groovy.grails.web.converters.marshaller.json.CollectionMarshaller());
        marshallers.add(new org.codehaus.groovy.grails.web.converters.marshaller.json.MapMarshaller());
        marshallers.add(new org.codehaus.groovy.grails.web.converters.marshaller.json.EnumMarshaller());
        marshallers.add(new org.codehaus.groovy.grails.web.converters.marshaller.ProxyUnwrappingMarshaller<JSON>());

        GrailsConfig grailsConfig = new GrailsConfig(application);
        if ("javascript".equals(grailsConfig.get("grails.converters.json.date", "default", Arrays.asList("javascript", "default")))) {
            LOG.debug("Using Javascript JSON Date Marshaller.");
            marshallers.add(new org.codehaus.groovy.grails.web.converters.marshaller.json.JavascriptDateMarshaller());
        }
        else {
            LOG.debug("Using default JSON Date Marshaller");
            marshallers.add(new org.codehaus.groovy.grails.web.converters.marshaller.json.DateMarshaller());
        }
        marshallers.add(new org.codehaus.groovy.grails.web.converters.marshaller.json.ToStringBeanMarshaller());

        boolean includeDomainVersion = includeDomainVersionProperty(grailsConfig,"json");
        ProxyHandler proxyHandler = getProxyHandler();
        if (grailsConfig.get("grails.converters.json.default.deep", false)) {
            LOG.debug("Using DeepDomainClassMarshaller as default.");
            marshallers.add(new org.codehaus.groovy.grails.web.converters.marshaller.json.DeepDomainClassMarshaller(includeDomainVersion, proxyHandler));
        }
        else {
            marshallers.add(new org.codehaus.groovy.grails.web.converters.marshaller.json.DomainClassMarshaller(includeDomainVersion, proxyHandler));
        }
        marshallers.add(new org.codehaus.groovy.grails.web.converters.marshaller.json.GroovyBeanMarshaller());
        marshallers.add(new org.codehaus.groovy.grails.web.converters.marshaller.json.GenericJavaBeanMarshaller());

        DefaultConverterConfiguration<JSON> cfg = new DefaultConverterConfiguration<JSON>(marshallers, proxyHandler);
        cfg.setEncoding(grailsConfig.get("grails.converters.encoding", "UTF-8"));
        String defaultCirRefBehaviour = grailsConfig.get("grails.converters.default.circular.reference.behaviour", "DEFAULT");
        cfg.setCircularReferenceBehaviour(Converter.CircularReferenceBehaviour.valueOf(
                grailsConfig.get("grails.converters.json.circular.reference.behaviour",
                      defaultCirRefBehaviour, Converter.CircularReferenceBehaviour.allowedValues())));

        Boolean defaultPrettyPrint = grailsConfig.get("grails.converters.default.pretty.print", false);
        Boolean prettyPrint = grailsConfig.get("grails.converters.json.pretty.print", defaultPrettyPrint);
        cfg.setPrettyPrint(prettyPrint);

        registerObjectMarshallersFromApplicationContext(cfg, JSON.class);

        ConvertersConfigurationHolder.setDefaultConfiguration(JSON.class, new ChainedConverterConfiguration<JSON>(cfg, proxyHandler));
    }

    private void initDeepJSONConfiguration(GrailsApplication application) {
        GrailsConfig grailsConfig = new GrailsConfig(application);
        DefaultConverterConfiguration<JSON> deepConfig = new DefaultConverterConfiguration<JSON>(ConvertersConfigurationHolder.getConverterConfiguration(JSON.class), getProxyHandler());
        deepConfig.registerObjectMarshaller(new org.codehaus.groovy.grails.web.converters.marshaller.json.DeepDomainClassMarshaller(includeDomainVersionProperty(grailsConfig, "json"), getProxyHandler()));
        ConvertersConfigurationHolder.setNamedConverterConfiguration(JSON.class, "deep", deepConfig);
    }

    private void initXMLConfiguration(GrailsApplication application) {
        LOG.debug("Initializing default XML Converters Configuration...");

        List<ObjectMarshaller<XML>> marshallers = new ArrayList<ObjectMarshaller<XML>>();
        marshallers.add(new org.codehaus.groovy.grails.web.converters.marshaller.xml.ArrayMarshaller());
        marshallers.add(new org.codehaus.groovy.grails.web.converters.marshaller.xml.CollectionMarshaller());
        marshallers.add(new org.codehaus.groovy.grails.web.converters.marshaller.xml.MapMarshaller());
        marshallers.add(new org.codehaus.groovy.grails.web.converters.marshaller.xml.EnumMarshaller());
        marshallers.add(new org.codehaus.groovy.grails.web.converters.marshaller.xml.DateMarshaller());
        marshallers.add(new ProxyUnwrappingMarshaller<XML>());
        marshallers.add(new org.codehaus.groovy.grails.web.converters.marshaller.xml.ToStringBeanMarshaller());
        ProxyHandler proxyHandler = getProxyHandler();

        GrailsConfig grailsConfig = new GrailsConfig(application);
        boolean includeDomainVersion = includeDomainVersionProperty(grailsConfig, "xml");
        if (grailsConfig.get("grails.converters.xml.default.deep", false)) {
            marshallers.add(new org.codehaus.groovy.grails.web.converters.marshaller.xml.DeepDomainClassMarshaller(includeDomainVersion, proxyHandler));
        }
        else {
            marshallers.add(new org.codehaus.groovy.grails.web.converters.marshaller.xml.DomainClassMarshaller(includeDomainVersion, proxyHandler));
        }
        marshallers.add(new org.codehaus.groovy.grails.web.converters.marshaller.xml.GroovyBeanMarshaller());
        marshallers.add(new org.codehaus.groovy.grails.web.converters.marshaller.xml.GenericJavaBeanMarshaller());

        DefaultConverterConfiguration<XML> cfg = new DefaultConverterConfiguration<XML>(marshallers,proxyHandler);
        cfg.setEncoding(grailsConfig.get("grails.converters.encoding", "UTF-8"));
        String defaultCirRefBehaviour = grailsConfig.get("grails.converters.default.circular.reference.behaviour", "DEFAULT");
        cfg.setCircularReferenceBehaviour(Converter.CircularReferenceBehaviour.valueOf(
                grailsConfig.get("grails.converters.xml.circular.reference.behaviour",
                      defaultCirRefBehaviour, Converter.CircularReferenceBehaviour.allowedValues())));

        Boolean defaultPrettyPrint = grailsConfig.get("grails.converters.default.pretty.print", false);
        Boolean prettyPrint = grailsConfig.get("grails.converters.xml.pretty.print", defaultPrettyPrint);
        cfg.setPrettyPrint(prettyPrint);
        registerObjectMarshallersFromApplicationContext(cfg, XML.class);
        ConvertersConfigurationHolder.setDefaultConfiguration(XML.class, new ChainedConverterConfiguration<XML>(cfg,proxyHandler));
    }

    private ProxyHandler getProxyHandler() {
        ProxyHandler proxyHandler;
        if (applicationContext != null) {
            proxyHandler = applicationContext.getBean(ProxyHandler.class);
        }
        else {
            proxyHandler = new DefaultProxyHandler();
        }
        return proxyHandler;
    }

    private void initDeepXMLConfiguration(GrailsApplication application) {
        GrailsConfig grailsConfig = new GrailsConfig(application);
        DefaultConverterConfiguration<XML> deepConfig = new DefaultConverterConfiguration<XML>(ConvertersConfigurationHolder.getConverterConfiguration(XML.class), getProxyHandler());
        deepConfig.registerObjectMarshaller(new org.codehaus.groovy.grails.web.converters.marshaller.xml.DeepDomainClassMarshaller(includeDomainVersionProperty(grailsConfig, "xml"), getProxyHandler()));
        ConvertersConfigurationHolder.setNamedConverterConfiguration(XML.class, "deep", deepConfig);
    }

    private boolean includeDomainVersionProperty(GrailsConfig grailsConfig, String converterType) {
        return grailsConfig.get(String.format("grails.converters.%s.domain.include.version", converterType),
                grailsConfig.get("grails.converters.domain.include.version", false));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <C extends Converter> void registerObjectMarshallersFromApplicationContext(
            DefaultConverterConfiguration<C> cfg, Class<C> converterClass) {

        if (applicationContext == null) {
            return;
        }

        for (Object o : applicationContext.getBeansOfType(ObjectMarshallerRegisterer.class).values()) {
            ObjectMarshallerRegisterer omr = (ObjectMarshallerRegisterer) o;
            if (omr.getConverterClass() == converterClass) {
                cfg.registerObjectMarshaller(omr.getMarshaller(), omr.getPriority());
            }
        }
    }
}
