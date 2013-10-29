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
package org.codehaus.groovy.grails.web.converters.marshaller.xml;

import grails.converters.XML;

import java.util.*;

import org.codehaus.groovy.grails.commons.*;
import org.codehaus.groovy.grails.support.IncludeExcludeSupport;
import org.codehaus.groovy.grails.support.proxy.EntityProxyHandler;
import org.codehaus.groovy.grails.support.proxy.ProxyHandler;
import org.codehaus.groovy.grails.web.converters.ConverterUtil;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.converters.marshaller.IncludeExcludePropertyMarshaller;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

/**
 *
 * Object marshaller for domain classes to XML
 *
 * @author Siegfried Puchbauer
 * @author Graeme Rocher
 * @since 1.1
 */
public class DomainClassMarshaller extends IncludeExcludePropertyMarshaller<XML> {

    protected final boolean includeVersion;
    protected ProxyHandler proxyHandler;
    protected GrailsApplication application;

    public DomainClassMarshaller(GrailsApplication application) {
        this(false, application);
    }

    public DomainClassMarshaller(boolean includeVersion, GrailsApplication application) {
        this.includeVersion = includeVersion;
        this.application = application;
    }

    public DomainClassMarshaller(boolean includeVersion, ProxyHandler proxyHandler, GrailsApplication application) {
        this(includeVersion, application);
        this.proxyHandler = proxyHandler;
    }

    public boolean supports(Object object) {
        String name = ConverterUtil.trimProxySuffix(object.getClass().getName());
        return application.isArtefactOfType(DomainClassArtefactHandler.TYPE, name);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void marshalObject(Object value, XML xml) throws ConverterException {
        Class clazz = value.getClass();

        List<String> excludes = xml.getExcludes(clazz);
        List<String> includes = xml.getIncludes(clazz);
        IncludeExcludeSupport<String> includeExcludeSupport = new IncludeExcludeSupport<String>();

        GrailsDomainClass domainClass = (GrailsDomainClass)application.getArtefact(
              DomainClassArtefactHandler.TYPE, ConverterUtil.trimProxySuffix(clazz.getName()));
        BeanWrapper beanWrapper = new BeanWrapperImpl(value);

        GrailsDomainClassProperty id = domainClass.getIdentifier();
        if(shouldInclude(includeExcludeSupport, includes, excludes,value, id.getName())) {
            Object idValue = beanWrapper.getPropertyValue(id.getName());

            if (idValue != null) xml.attribute("id", String.valueOf(idValue));
        }

        if (shouldInclude(includeExcludeSupport, includes, excludes, value, GrailsDomainClassProperty.VERSION) && includeVersion) {
            Object versionValue = beanWrapper.getPropertyValue(domainClass.getVersion().getName());
            xml.attribute("version", String.valueOf(versionValue));
        }

        GrailsDomainClassProperty[] properties = domainClass.getPersistentProperties();

        for (GrailsDomainClassProperty property : properties) {
            String propertyName = property.getName();
            if(!shouldInclude(includeExcludeSupport, includes, excludes, value, property.getName())) continue;

            xml.startNode(propertyName);
            if (!property.isAssociation()) {
                // Write non-relation property
                Object val = beanWrapper.getPropertyValue(propertyName);
                xml.convertAnother(val);
            }
            else {
                if (isRenderDomainClassRelations()) {
                    Object referenceObject = beanWrapper.getPropertyValue(propertyName);
                    if (referenceObject != null && shouldInitializeProxy(referenceObject)) {
                        referenceObject = proxyHandler.unwrapIfProxy(referenceObject);
                        if (referenceObject instanceof SortedMap) {
                            referenceObject = new TreeMap((SortedMap) referenceObject);
                        }
                        else if (referenceObject instanceof SortedSet) {
                            referenceObject = new TreeSet((SortedSet) referenceObject);
                        }
                        else if (referenceObject instanceof Set) {
                            referenceObject = new HashSet((Set) referenceObject);
                        }
                        else if (referenceObject instanceof Map) {
                            referenceObject = new HashMap((Map) referenceObject);
                        }
                        else if (referenceObject instanceof Collection) {
                            referenceObject = new ArrayList((Collection) referenceObject);
                        }
                        xml.convertAnother(referenceObject);
                    }
                }
                else {
                    Object referenceObject = beanWrapper.getPropertyValue(propertyName);
                    if (referenceObject != null) {
                        GrailsDomainClass referencedDomainClass = property.getReferencedDomainClass();

                        // Embedded are now always fully rendered
                        if (referencedDomainClass == null || property.isEmbedded() || GrailsClassUtils.isJdk5Enum(property.getType())) {
                            xml.convertAnother(referenceObject);
                        }
                        else if (property.isOneToOne() || property.isManyToOne() || property.isEmbedded()) {
                            asShortObject(referenceObject, xml, referencedDomainClass.getIdentifier(), referencedDomainClass);
                        }
                        else {
                            GrailsDomainClassProperty referencedIdProperty = referencedDomainClass.getIdentifier();
                            @SuppressWarnings("unused")
                            String refPropertyName = referencedDomainClass.getPropertyName();
                            if (referenceObject instanceof Collection) {
                                Collection o = (Collection) referenceObject;
                                for (Object el : o) {
                                    xml.startNode(xml.getElementName(el));
                                    asShortObject(el, xml, referencedIdProperty, referencedDomainClass);
                                    xml.end();
                                }
                            }
                            else if (referenceObject instanceof Map) {
                                Map<Object, Object> map = (Map<Object, Object>) referenceObject;
                                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                                    String key = String.valueOf(entry.getKey());
                                    Object o = entry.getValue();
                                    xml.startNode("entry").attribute("key", key);
                                    asShortObject(o, xml, referencedIdProperty, referencedDomainClass);
                                    xml.end();
                                }
                            }
                        }
                    }
                }
            }
            xml.end();
        }
    }

    private boolean shouldInclude(IncludeExcludeSupport<String> includeExcludeSupport, List<String> includes, List<String> excludes, Object object, String name) {
        return includeExcludeSupport.shouldInclude(includes, excludes, name) && shouldInclude(object,name);
    }

    private boolean shouldInitializeProxy(Object object) {
        return proxyHandler.isInitialized(object) || shouldInitializeProxies();
    }

    protected boolean shouldInitializeProxies() {
        return true;
    }


    protected void asShortObject(Object refObj, XML xml, GrailsDomainClassProperty idProperty,
            GrailsDomainClass referencedDomainClass) throws ConverterException {
        Object idValue;
        if (proxyHandler instanceof EntityProxyHandler) {
            idValue = ((EntityProxyHandler) proxyHandler).getProxyIdentifier(refObj);
            if (idValue == null) {
                ClassPropertyFetcher propertyFetcher = ClassPropertyFetcher.forClass(refObj.getClass());
                idValue = propertyFetcher.getPropertyValue(refObj, idProperty.getName());
            }
        }
        else {
            ClassPropertyFetcher propertyFetcher = ClassPropertyFetcher.forClass(refObj.getClass());
            idValue = propertyFetcher.getPropertyValue(refObj, idProperty.getName());
        }
        xml.attribute(GrailsDomainClassProperty.IDENTITY,String.valueOf(idValue));
    }

    protected boolean isRenderDomainClassRelations() {
        return false;
    }
}
