/* Copyright 2006-2007 Graeme Rocher
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
package org.codehaus.groovy.grails.web.converters.xtream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.web.converters.ConverterUtil;
import org.hibernate.collection.AbstractPersistentCollection;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.*;

/**
 * An XStream converter for converting Grails domain classes to XML
 *
 * @author Siegfried Puchbauer
 */
public class DomainClassConverter implements Converter {

    private boolean renderDomainClassRelations = false;

    public boolean isRenderDomainClassRelations() {
        return renderDomainClassRelations;
    }

    /**
     * Configure wheter DomainClass Relations should be fully rendered or on the referenced ID's
     *
     * @param renderDomainClassRelations true to render nested DomainClass Instances, false to render their ID's
     */
    public void setRenderDomainClassRelations(boolean renderDomainClassRelations) {
        this.renderDomainClassRelations = renderDomainClassRelations;
    }

    public void marshal(Object value, HierarchicalStreamWriter writer,
                        MarshallingContext context) {

        Class clazz = value.getClass();
        GrailsDomainClass domainClass = ConverterUtil.getDomainClass(clazz.getName());
        BeanWrapper beanWrapper = new BeanWrapperImpl(value);


        GrailsDomainClassProperty id = domainClass.getIdentifier();

        Object idValue = extractIdValue(value, id);
        if (idValue != null) writer.addAttribute("id", String.valueOf(idValue));

        GrailsDomainClassProperty[] properties = domainClass.getPersistentProperties();

        for (int i = 0; i < properties.length; i++) {
            GrailsDomainClassProperty property = properties[i];
            writer.startNode(property.getName());
            if (!property.isAssociation()) {
                // Write non-relation property
                Object val = beanWrapper.getPropertyValue(property.getName());
                if (val == null) {
                    writer.startNode("null");
                    writer.endNode();
                } else {
                    context.convertAnother(val);
                }
            } else {
                Object referenceObject = beanWrapper.getPropertyValue(property.getName());
                if (isRenderDomainClassRelations()) {
                    if (referenceObject == null) {
                        writer.startNode("null");
                        writer.endNode();
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
                        }
                        context.convertAnother(referenceObject);
                    }
                } else {
                    if (referenceObject == null) {
                        writer.startNode("null");
                        writer.endNode();
                    } else {
                        GrailsDomainClass referencedDomainClass = property.getReferencedDomainClass();
                        GrailsDomainClassProperty referencedIdProperty = referencedDomainClass.getIdentifier();
                        if (property.isOneToOne() || property.isManyToOne() || property.isEmbedded()) {
                            // Property contains 1 foreign Domain Object
                            writer.addAttribute("id", String.valueOf(extractIdValue(referenceObject, referencedIdProperty)));
                        } else {
                            String refPropertyName = referencedDomainClass.getPropertyName();
                            if (referenceObject instanceof Collection) {
                                Collection o = (Collection) referenceObject;
                                for (Iterator it = o.iterator(); it.hasNext();) {
                                    Object el = (Object) it.next();
                                    writer.startNode(refPropertyName);
                                    writer.addAttribute("id", String.valueOf(extractIdValue(el, referencedIdProperty)));
                                    writer.endNode();
                                }

                            } else if (referenceObject instanceof Map) {
                                Map map = (Map) referenceObject;
                                Iterator iterator = map.keySet().iterator();
                                while (iterator.hasNext()) {
                                    String key = String.valueOf(iterator.next());
                                    Object o = map.get(key);
                                    writer.startNode("entry");
                                    writer.startNode("string"); // key of map entry has to be a string
                                    writer.setValue(key);
                                    writer.endNode(); // end string
                                    writer.startNode(refPropertyName);
                                    writer.addAttribute("id", String.valueOf(extractIdValue(o, referencedIdProperty)));
                                    writer.endNode(); // end refPropertyName
                                    writer.endNode(); // end entry
                                }
                            }
                        }
                    }
                }
            }
            writer.endNode();
        }
    }

    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {
//        Under development:
//        try {
//            Class clazz = context.getRequiredType();
//            GrailsDomainClass domainClass = ConverterUtil.getDomainClass(clazz.getName());
//            Object o = clazz.newInstance();
//            BeanWrapper beanWrapper = new BeanWrapperImpl(o);
//            //GroovyObject go = (GroovyObject) clazz.newInstance();
//
//            while (reader.hasMoreChildren()) {
//                reader.moveDown();
//                String propertyName = reader.getNodeName();
//                GrailsDomainClassProperty property = domainClass.getPropertyByName(propertyName);
//
//                if (property.isAssociation()) {
//                    //  TODO
//                } else {
//                    Object value = context.convertAnother(o, property.getType());
//                    beanWrapper.setPropertyValue(property.getName(), value);
//                }
//                reader.moveUp();
//            }
//
//            return null;
//        } catch (Exception e) {
//            throw new UnhandledException(e);
//        }
        return null;
    }

    public boolean canConvert(Class clazz) {
        return ConverterUtil.isDomainClass(clazz);
    }

    private Object extractIdValue(Object domainObject, GrailsDomainClassProperty idProperty) {
        BeanWrapper beanWrapper = new BeanWrapperImpl(domainObject);
        return beanWrapper.getPropertyValue(idProperty.getName());
    }

}
