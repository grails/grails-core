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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.web.converters.ConverterUtil;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller;
import org.hibernate.Hibernate;
import org.hibernate.collection.AbstractPersistentCollection;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.*;

/**
 * @author Siegfried Puchbauer
 * @since 1.1
 */
public class DomainClassMarshaller implements ObjectMarshaller<XML> {

    private final Log log = LogFactory.getLog(getClass());

    public boolean supports(Object object) {
        return ConverterUtil.isDomainClass(object.getClass());
    }

    public void marshalObject(Object value, XML xml) throws ConverterException {
        Class clazz = value.getClass();
        GrailsDomainClass domainClass = ConverterUtil.getDomainClass(clazz.getName());
        BeanWrapper beanWrapper = new BeanWrapperImpl(value);

        GrailsDomainClassProperty id = domainClass.getIdentifier();
        Object idValue = extractIdValue(value, id);

        if (idValue != null) xml.attribute("id", String.valueOf(idValue));

        GrailsDomainClassProperty[] properties = domainClass.getPersistentProperties();

        for (GrailsDomainClassProperty property : properties) {
            xml.startNode(property.getName());
            if (!property.isAssociation()) {
                // Write non-relation property
                Object val = beanWrapper.getPropertyValue(property.getName());
                xml.convertAnother(val);
            } else {
                Object referenceObject = beanWrapper.getPropertyValue(property.getName());
                if (isRenderDomainClassRelations()) {
                    if (referenceObject == null) {
                    } else {
                        if (referenceObject instanceof AbstractPersistentCollection) {
                            // Force initialisation and get a non-persistent Collection Type
                            AbstractPersistentCollection acol = (AbstractPersistentCollection) referenceObject;
                            acol.forceInitialization();
                            if (referenceObject instanceof SortedMap) {
                                referenceObject = new TreeMap((SortedMap) referenceObject);
                            } else if (referenceObject instanceof SortedSet) {
                                referenceObject = new TreeSet((SortedSet) referenceObject);
                            } else if (referenceObject instanceof Set) {
                                referenceObject = new HashSet((Set) referenceObject);
                            } else if (referenceObject instanceof Map) {
                                referenceObject = new HashMap((Map) referenceObject);
                            } else {
                                referenceObject = new ArrayList((Collection) referenceObject);
                            }
                        } else if(!Hibernate.isInitialized(referenceObject)) {
                            Hibernate.initialize(referenceObject);
                        }
                        xml.convertAnother(referenceObject);
                    }
                } else {
                    if (referenceObject != null) {
                        GrailsDomainClass referencedDomainClass = property.getReferencedDomainClass();

                        if (property.isOneToOne() || property.isManyToOne() || property.isEmbedded()) {
                            // Property contains 1 foreign Domain Object
                            if(GrailsClassUtils.isJdk5Enum(property.getType())) {
                                xml.convertAnother(referenceObject);
                            } else {
                                asShortObject(referenceObject, xml, referencedDomainClass.getIdentifier(), referencedDomainClass);
                            }
                        } else {
                            GrailsDomainClassProperty referencedIdProperty = referencedDomainClass.getIdentifier();
                            String refPropertyName = referencedDomainClass.getPropertyName();
                            if (referenceObject instanceof Collection) {
                                Collection o = (Collection) referenceObject;
                                for (Object el : o) {
                                    xml.startNode(xml.getElementName(el));
                                    asShortObject(el, xml, referencedIdProperty, referencedDomainClass);
                                    xml.end();
                                }

                            } else if (referenceObject instanceof Map) {
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


    protected void asShortObject(Object refObj, XML xml, GrailsDomainClassProperty idProperty, GrailsDomainClass referencedDomainClass) throws ConverterException {
        xml.attribute("id",String.valueOf(extractIdValue(refObj, idProperty)));
    }

    protected Object extractIdValue(Object domainObject, GrailsDomainClassProperty idProperty) {
        BeanWrapper beanWrapper = new BeanWrapperImpl(domainObject);
        return beanWrapper.getPropertyValue(idProperty.getName());
    }


//    public void oldmarshal(Object o, JSON json) throws ConverterException {
//        JSONWriter writer = json.getWriter();
//        try {
//            BeanWrapper beanWrapper = ConverterUtil.createBeanWrapper(o);
//            GrailsDomainClass domainClass = ConverterUtil.getDomainClass(o.getClass().getName());
//            if (domainClass != null) {
//
//                writer.object();
//                json.property("class", domainClass.getName());
//                GrailsDomainClassProperty id = domainClass.getIdentifier();
//                json.property(id.getName(), beanWrapper.getPropertyValue(id.getName()));
//                GrailsDomainClassProperty[] properties = domainClass.getPersistentProperties();
//                for (GrailsDomainClassProperty prop : properties) {
//                    if (!prop.isAssociation() || isRenderDomainClassRelations()) {
//                        json.property(prop.getName(), beanWrapper.getPropertyValue(prop.getName()));
//                    } else {
//                        writer.key(prop.getName());
//                        Object refValue = beanWrapper.getPropertyValue(prop.getName());
//                        if (log.isDebugEnabled()) {
//                            log.debug("Serializing isAssociation prop: " + DefaultGroovyMethods.inspect(refValue));
//                        }
//                        if (refValue == null) {
//                            log.debug("refValue is null");
//                            Class propClass = prop.getType();
//                            if (Collection.class.isAssignableFrom(propClass)) {
//                                writer.array();
//                                writer.endArray();
//                            } else if (Map.class.isAssignableFrom(propClass)) {
//                                writer.object();
//                                writer.endObject();
//                            } else {
//                                writer.value(null);
//                            }
//                        } else if (prop.isOneToOne() || prop.isManyToOne() || prop.isEmbedded()) {
//                            log.debug(".isOneToOne() || prop.isManyToOne() || prop.isEmbedded()");
//                            writer.object();
//                            writer.key("class").value(prop.getType().getName());
//                            writer.key("id");
//                            json.value(extractIdValue(refValue, prop.getReferencedDomainClass().getIdentifier()));
//                            writer.endObject();
//                        } else {
//                            GrailsDomainClassProperty refIdProperty = prop.getReferencedDomainClass().getIdentifier();
//                            if (Collection.class.isAssignableFrom(prop.getType())) {
//                                log.debug("Collection");
//                                writer.array();
//                                Collection col = (Collection) refValue;
//                                for (Object val : col) {
//                                    if (val != null) {
//                                        writer.object();
//                                        writer.key("class").value(val.getClass().getName());
//                                        writer.key("id");
//                                        json.value(extractIdValue(val, refIdProperty));
//                                        writer.endObject();
//                                    } else {
//                                        writer.value(null);
//                                    }
//                                }
//                                writer.endArray();
//                            } else if (Map.class.isAssignableFrom(prop.getType())) {
//                                log.debug("Map");
//                                writer.object();
//                                Map<String, Object> map = (Map<String, Object>) refValue;
//                                for (Map.Entry<String, Object> entry : map.entrySet()) {
//                                    writer.key(entry.getKey());
//                                    Object mo = entry.getValue();
//                                    if (mo != null) {
//                                        writer.object();
//                                        writer.key("class").value(mo.getClass().getName());
//                                        writer.key("id");
//                                        json.value(extractIdValue(mo, refIdProperty));
//                                        writer.endObject();
//                                    } else {
//                                        writer.value(null);
//                                    }
//                                }
//                                writer.endObject();
//                            } else {
//                                throw new ConverterException(String.format(
//                                        "Unable to convert property \"%s\" of Domain Class \"%s\": The association class [%s] is not a Collection or a Map!",
//                                        prop.getName(), domainClass.getName(), prop.getType().getName()
//                                ));
//                            }
//                        }
//                    }
//                }
//                writer.endObject();
//            }
//        } catch (ConverterException ce) {
//            throw ce;
//        } catch (
//                JSONException e) {
//            throw new ConverterException(e);
//        }
//    }

    protected boolean isRenderDomainClassRelations() {
        return false;
    }
}