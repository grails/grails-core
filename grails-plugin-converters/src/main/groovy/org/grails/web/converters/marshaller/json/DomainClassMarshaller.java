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
package org.grails.web.converters.marshaller.json;

import grails.converters.JSON;
import groovy.lang.GroovyObject;

import java.util.*;

import org.grails.core.artefact.DomainClassArtefactHandler;

import grails.core.GrailsApplication;
import grails.core.GrailsDomainClass;
import grails.core.GrailsDomainClassProperty;

import org.grails.core.util.IncludeExcludeSupport;
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher;
import org.grails.web.converters.ConverterUtil;
import org.grails.web.converters.exceptions.ConverterException;
import org.grails.web.converters.marshaller.IncludeExcludePropertyMarshaller;

import grails.core.support.proxy.DefaultProxyHandler;
import grails.core.support.proxy.EntityProxyHandler;
import grails.core.support.proxy.ProxyHandler;

import org.grails.web.json.JSONWriter;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

/**
 *
 * Object marshaller for domain classes to JSON
 *
 * @author Siegfried Puchbauer
 * @author Graeme Rocher
 *
 * @since 1.1
 */
public class DomainClassMarshaller extends IncludeExcludePropertyMarshaller<JSON> {

    private boolean includeVersion = false;
    private boolean includeClass = false;
    private ProxyHandler proxyHandler;
    private GrailsApplication application;

    public DomainClassMarshaller(boolean includeVersion, GrailsApplication application) {
        this(includeVersion, new DefaultProxyHandler(), application);
    }

    public DomainClassMarshaller(boolean includeVersion, ProxyHandler proxyHandler, GrailsApplication application) {
        this(includeVersion, false, proxyHandler, application);
    }

    public DomainClassMarshaller(boolean includeVersion, boolean includeClass, ProxyHandler proxyHandler, GrailsApplication application) {
        this.includeVersion = includeVersion;
        this.includeClass = includeClass;
        this.proxyHandler = proxyHandler;
        this.application = application;
    }

    public boolean isIncludeVersion() {
        return includeVersion;
    }

    public boolean isIncludeClass() {
        return includeClass;
    }

    public void setIncludeClass(boolean includeClass) {
        this.includeClass = includeClass;
    }

    public void setIncludeVersion(boolean includeVersion) {
        this.includeVersion = includeVersion;
    }

    public boolean supports(Object object) {
        String name = ConverterUtil.trimProxySuffix(object.getClass().getName());
        return application.isArtefactOfType(DomainClassArtefactHandler.TYPE, name);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void marshalObject(Object value, JSON json) throws ConverterException {
        JSONWriter writer = json.getWriter();
        value = proxyHandler.unwrapIfProxy(value);
        Class<?> clazz = value.getClass();

        List<String> excludes = json.getExcludes(clazz);
        List<String> includes = json.getIncludes(clazz);
        IncludeExcludeSupport<String> includeExcludeSupport = new IncludeExcludeSupport<String>();

        GrailsDomainClass domainClass = (GrailsDomainClass)application.getArtefact(
              DomainClassArtefactHandler.TYPE, ConverterUtil.trimProxySuffix(clazz.getName()));
        BeanWrapper beanWrapper = new BeanWrapperImpl(value);

        writer.object();

        if(includeClass && shouldInclude(includeExcludeSupport, includes, excludes, value, "class")) {
            writer.key("class").value(domainClass.getClazz().getName());
        }


        GrailsDomainClassProperty id = domainClass.getIdentifier();

        if(shouldInclude(includeExcludeSupport, includes, excludes, value, id.getName())) {
            Object idValue = extractValue(value, id);
            if(idValue != null) {
                json.property(GrailsDomainClassProperty.IDENTITY, idValue);
            }
        }

        if (shouldInclude(includeExcludeSupport, includes, excludes, value, GrailsDomainClassProperty.VERSION) && isIncludeVersion()) {
            GrailsDomainClassProperty versionProperty = domainClass.getVersion();
            Object version = extractValue(value, versionProperty);
            if(version != null) {
                json.property(GrailsDomainClassProperty.VERSION, version);
            }
        }

        GrailsDomainClassProperty[] properties = domainClass.getPersistentProperties();

        for (GrailsDomainClassProperty property : properties) {
            if(!shouldInclude(includeExcludeSupport, includes, excludes, value, property.getName())) continue;

            writer.key(property.getName());
            if (!property.isAssociation()) {
                // Write non-relation property
                Object val = beanWrapper.getPropertyValue(property.getName());
                json.convertAnother(val);
            }
            else {
                Object referenceObject = beanWrapper.getPropertyValue(property.getName());
                if (isRenderDomainClassRelations()) {
                    if (referenceObject == null) {
                        writer.valueNull();
                    }
                    else {
                        referenceObject = proxyHandler.unwrapIfProxy(referenceObject);
                        if (referenceObject instanceof SortedMap) {
                            referenceObject = new TreeMap((SortedMap) referenceObject);
                        }
                        else if (referenceObject instanceof SortedSet) {
                            referenceObject = new TreeSet((SortedSet) referenceObject);
                        }
                        else if (referenceObject instanceof Set) {
                            referenceObject = new LinkedHashSet((Set) referenceObject);
                        }
                        else if (referenceObject instanceof Map) {
                            referenceObject = new LinkedHashMap((Map) referenceObject);
                        }
                        else if (referenceObject instanceof Collection) {
                            referenceObject = new ArrayList((Collection) referenceObject);
                        }
                        json.convertAnother(referenceObject);
                    }
                }
                else {
                    if (referenceObject == null) {
                        json.value(null);
                    }
                    else {
                        GrailsDomainClass referencedDomainClass = property.getReferencedDomainClass();

                        // Embedded are now always fully rendered
                        if (referencedDomainClass == null || property.isEmbedded() || property.getType().isEnum()) {
                            json.convertAnother(referenceObject);
                        }
                        else if (property.isOneToOne() || property.isManyToOne() || property.isEmbedded()) {
                            asShortObject(referenceObject, json, referencedDomainClass.getIdentifier(), referencedDomainClass);
                        }
                        else {
                            GrailsDomainClassProperty referencedIdProperty = referencedDomainClass.getIdentifier();
                            @SuppressWarnings("unused")
                            String refPropertyName = referencedDomainClass.getPropertyName();
                            if (referenceObject instanceof Collection) {
                                Collection o = (Collection) referenceObject;
                                writer.array();
                                for (Object el : o) {
                                    asShortObject(el, json, referencedIdProperty, referencedDomainClass);
                                }
                                writer.endArray();
                            }
                            else if (referenceObject instanceof Map) {
                                Map<Object, Object> map = (Map<Object, Object>) referenceObject;
                                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                                    String key = String.valueOf(entry.getKey());
                                    Object o = entry.getValue();
                                    writer.object();
                                    writer.key(key);
                                    asShortObject(o, json, referencedIdProperty, referencedDomainClass);
                                    writer.endObject();
                                }
                            }
                        }
                    }
                }
            }
        }
        writer.endObject();
    }

    private boolean shouldInclude(IncludeExcludeSupport<String> includeExcludeSupport, List<String> includes, List<String> excludes, Object object, String propertyName) {
        return includeExcludeSupport.shouldInclude(includes,excludes,propertyName) && shouldInclude(object,propertyName);
    }

    protected void asShortObject(Object refObj, JSON json, GrailsDomainClassProperty idProperty, GrailsDomainClass referencedDomainClass) throws ConverterException {

        Object idValue;

        if (proxyHandler instanceof EntityProxyHandler) {
            idValue = ((EntityProxyHandler) proxyHandler).getProxyIdentifier(refObj);
            if (idValue == null) {
                idValue = extractValue(refObj, idProperty);
            }
        }
        else {
            idValue = extractValue(refObj, idProperty);
        }
        JSONWriter writer = json.getWriter();
        writer.object();
        if(isIncludeClass()) {
            writer.key("class").value(referencedDomainClass.getFullName());
        }
        if(idValue != null) {
            writer.key("id").value(idValue);
        }
        writer.endObject();
    }

    protected Object extractValue(Object domainObject, GrailsDomainClassProperty property) {
        if(domainObject instanceof GroovyObject) {
            return ((GroovyObject)domainObject).getProperty(property.getName());
        }
        else {
            ClassPropertyFetcher propertyFetcher = ClassPropertyFetcher.forClass(domainObject.getClass());
            return propertyFetcher.getPropertyValue(domainObject, property.getName());
        }
    }

    protected boolean isRenderDomainClassRelations() {
        return false;
    }
}
