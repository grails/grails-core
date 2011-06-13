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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.support.proxy.EntityProxyHandler;
import org.codehaus.groovy.grails.support.proxy.ProxyHandler;
import org.codehaus.groovy.grails.web.converters.ConverterUtil;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

/**
 * @author Siegfried Puchbauer
 * @since 1.1
 */
public class DomainClassMarshaller implements ObjectMarshaller<XML> {

    private final boolean includeVersion;
    private ProxyHandler proxyHandler;
    private GrailsApplication application;

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
        GrailsDomainClass domainClass = (GrailsDomainClass)application.getArtefact(
              DomainClassArtefactHandler.TYPE, ConverterUtil.trimProxySuffix(clazz.getName()));
        BeanWrapper beanWrapper = new BeanWrapperImpl(value);

        GrailsDomainClassProperty id = domainClass.getIdentifier();
        Object idValue = beanWrapper.getPropertyValue(id.getName());

        if (idValue != null) xml.attribute("id", String.valueOf(idValue));

        if (includeVersion) {
            Object versionValue = beanWrapper.getPropertyValue(domainClass.getVersion().getName());
            xml.attribute("version", String.valueOf(versionValue));
        }

        GrailsDomainClassProperty[] properties = domainClass.getPersistentProperties();

        for (GrailsDomainClassProperty property : properties) {
            xml.startNode(property.getName());
            if (!property.isAssociation()) {
                // Write non-relation property
                Object val = beanWrapper.getPropertyValue(property.getName());
                xml.convertAnother(val);
            }
            else {
                Object referenceObject = beanWrapper.getPropertyValue(property.getName());
                if (isRenderDomainClassRelations()) {
                    if (referenceObject != null) {
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

    protected void asShortObject(Object refObj, XML xml, GrailsDomainClassProperty idProperty,
            @SuppressWarnings("unused") GrailsDomainClass referencedDomainClass) throws ConverterException {
        Object idValue;
        if (proxyHandler instanceof EntityProxyHandler) {

            idValue = ((EntityProxyHandler) proxyHandler).getProxyIdentifier(refObj);
            if (idValue == null) {
                idValue = new BeanWrapperImpl(refObj).getPropertyValue(idProperty.getName());
            }

        }
        else {
            idValue = new BeanWrapperImpl(refObj).getPropertyValue(idProperty.getName());
        }
        xml.attribute("id",String.valueOf(idValue));
    }

    protected boolean isRenderDomainClassRelations() {
        return false;
    }
}
