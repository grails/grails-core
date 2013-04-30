/*
 * Copyright 2013 the original author or authors.
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
package org.codehaus.groovy.grails.web.binding

import grails.util.Environment
import grails.util.GrailsNameUtils
import grails.validation.DeferredBindingActions
import groovy.transform.CompileStatic
import groovy.util.slurpersupport.GPathResult

import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

import org.codehaus.groovy.grails.commons.AnnotationDomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.commons.GrailsMetaClassUtils
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.binding.converters.ByteArrayMultipartFileValueConverter
import org.codehaus.groovy.runtime.InvokerHelper
import org.grails.databinding.SimpleDataBinder
import org.grails.databinding.converters.FormattedValueConverter
import org.grails.databinding.converters.ValueConverter
import org.grails.databinding.events.DataBindingListener
import org.grails.databinding.xml.GPathResultMap
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
class GormAwareDataBinder extends SimpleDataBinder {
    protected static final Map<Class, List> CLASS_TO_BINDING_INCLUDE_LIST = new ConcurrentHashMap<Class, List>()
    protected GrailsApplication grailsApplication

    GormAwareDataBinder(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
        this.conversionService = new SpringConversionServiceAdapter()
        registerConverter new ByteArrayMultipartFileValueConverter()
    }

    /**
     * @param obj the object to perform data binding on
     * @param source a Map containg the values to be bound to obj
     */
    void bind(obj, Map source) {
        bind obj, source, null, getBindingIncludeList(obj), null, null
    }

    /**
     * @param obj the object to perform data binding on
     * @param source a Map containg the values to be bound to obj
     * @param listener will be notified of data binding events
     */
    void bind(obj, Map source, DataBindingListener listener) {
        bind obj, source, null, getBindingIncludeList(obj), null, listener
    }

    /**
     * @param obj the object to perform data binding on
     * @param gpath contains an XML representation of the data to be bound to obj
     */
    void bind(obj, GPathResult gpath) {
        bind obj, new GPathResultMap(gpath), getBindingIncludeList(obj)
    }

    @Override
    protected Class<?> getReferencedTypeForCollection(String name, Object target) {
        def referencedType = null
        if (grailsApplication != null) {
            def dc = (GrailsDomainClass) grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, target.getClass().name)
            if (dc != null) {
                def domainProperty = dc.getPersistentProperty(name)
                if (domainProperty != null) {
                    referencedType = domainProperty.referencedPropertyType
                }
            }
        }
        referencedType ?: super.getReferencedTypeForCollection(name, target)
    }

    @Override
    protected initializeProperty(obj, String propName, Class propertyType, Map<String, Object> source) {
        def isInitialized = false
        def isDomainClass = isDomainClass propertyType
        if(isDomainClass && source.containsKey(propName)) {
            def val = source[propName]
            if(val instanceof Map && val.containsKey('id')) {
                def persistentInstance = getPersistentInstance(propertyType, val['id'])
                if(persistentInstance != null) {
                    obj[propName] = persistentInstance
                    isInitialized = true
                }
            }
        }
        if(!isInitialized) {
            super.initializeProperty obj, propName,  propertyType, source
        }
    }

    protected getPersistentInstance(Class<?> type, id) {
        InvokerHelper.invokeStaticMethod type, 'get', id
    }

    protected boolean isDomainClass(final Class<?> clazz) {
        return DomainClassArtefactHandler.isDomainClass(clazz) || AnnotationDomainClassArtefactHandler.isJPADomainClass(clazz)
    }

    @Override
    protected processProperty(obj, String propName, val, Map source,  List whiteList, List blackList, DataBindingListener listener) {
        if(val instanceof Map && val.containsKey('id')) {
            def idValue = val['id']
            if(idValue instanceof GString) {
                idValue = idValue.toString()
            }
            def descriptor = getIndexedPropertyReferenceDescriptor propName
            if(descriptor) {
                def metaProperty = obj.metaClass.getMetaProperty descriptor.propertyName
                if(metaProperty) {
                    def referencedType = getReferencedTypeForCollection descriptor.propertyName, obj
                    if(referencedType != null) {
                        if(Collection.isAssignableFrom(metaProperty.type)) {
                            def collection = initializeCollection obj, descriptor.propertyName, metaProperty.type
                            addElementToCollectionAt obj, descriptor.propertyName, collection, Integer.parseInt(descriptor.index), 'null' == idValue ? null : getPersistentInstance(referencedType, idValue)
                        } else if(Map.isAssignableFrom(metaProperty.type)) {
                            Map map = (Map)obj[descriptor.propertyName]
                            if(idValue == 'null' || idValue == null || idValue == '') {
                                if(map != null) {
                                    map.remove descriptor.index
                                }
                            } else {
                                map = initializeMap obj, descriptor.propertyName
                                def persistedInstance = getPersistentInstance referencedType, idValue
                                if(persistedInstance != null) {
                                    map[descriptor.index] = persistedInstance
                                    bind persistedInstance, val, listener
                                } else {
                                    map.remove descriptor.index
                                }
                            }
                        }
                    }
                }
            } else {
                if(isOkToBind(propName, whiteList, blackList)) {
                    def metaProperty = obj.metaClass.getMetaProperty propName
                    if(metaProperty) {
                        def persistedInstance = null
                        if(idValue != 'null' && idValue != null && idValue != '') {
                            persistedInstance = getPersistentInstance(((MetaBeanProperty)metaProperty).field.type, idValue)
                            if(persistedInstance == null) {
                                persistedInstance = ((MetaBeanProperty)metaProperty).field.type.newInstance()
                            }
                        }

                        setPropertyValue obj, source, propName, persistedInstance, listener
                        if(persistedInstance != null) {
                            bind persistedInstance, val, listener
                        }
                    }
                }
            }
        } else {
        /*
            if(grailsApplication != null) {
                def domainClass = (GrailsDomainClass)grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, obj.getClass().name)
                if(domainClass != null) {
                    def property = domainClass.getPersistentProperty(propName)
                    if (property != null) {
                        println property
                    }
                }
            }
            */
            super.processProperty obj, propName, val, source, whiteList, blackList, listener
        }
    }

    private static List getBindingIncludeList(final Object object) {
        List includeList = Collections.EMPTY_LIST
        try {
            final Class<? extends Object> objectClass = object.getClass()
            if (CLASS_TO_BINDING_INCLUDE_LIST.containsKey(objectClass)) {
                includeList = CLASS_TO_BINDING_INCLUDE_LIST.get objectClass
            } else {
                def whiteListField = objectClass.getDeclaredField DefaultASTDatabindingHelper.DEFAULT_DATABINDING_WHITELIST
                if (whiteListField != null) {
                    if ((whiteListField.getModifiers() & Modifier.STATIC) != 0) {
                        final Object whiteListValue = whiteListField.get objectClass
                        if (whiteListValue instanceof List) {
                            includeList = (List)whiteListValue
                        }
                    }
                }
                if (!Environment.getCurrent().isReloadEnabled()) {
                    CLASS_TO_BINDING_INCLUDE_LIST.put objectClass, includeList
                }
            }
        } catch (Exception e) {
        }
        includeList
    }

    @Override
    protected addElementToCollectionAt(obj, String propertyName, Collection collection, index, val) {
        super.addElementToCollectionAt obj, propertyName, collection, index, val

        if (grailsApplication == null) {
            return
        }

        def domainClass = (GrailsDomainClass)grailsApplication.getArtefact('Domain', obj.getClass().name)
        if(domainClass != null) {
            def property = domainClass.getPersistentProperty propertyName
            if (property != null && property.isBidirectional()) {
                def otherSide = property.otherSide
                if (otherSide.isManyToOne()) {
                    val[otherSide.name] = obj
                }
            }
        }
    }

    @Override
    protected setPropertyValue(obj, Map source, String propName, propertyValue, DataBindingListener listener) {
        boolean isSet = false
        if(grailsApplication != null) {
            def domainClass = (GrailsDomainClass)grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, obj.getClass().name)
            if(domainClass != null) {
                def property = domainClass.getPersistentProperty propName
                if (property != null) {
                    if(Collection.isAssignableFrom(property.type)) {
                        if(propertyValue instanceof String) {
                            isSet = addElementToCollection obj, propName, property, propertyValue, true
                        } else if(propertyValue instanceof String[]){
                            if(isDomainClass(property.referencedPropertyType)) {
                                propertyValue.each { val ->
                                    isSet = isSet | addElementToCollection(obj, propName, property, val, false)
                                }
                            }
                        }
                    }
                    def otherSide = property.getOtherSide()
                    if (otherSide != null && List.isAssignableFrom(otherSide.getType()) && !property.isOptional()) {
                        DeferredBindingActions.addBindingAction(new Runnable() {
                            void run() {
                                if (otherSide.isOneToMany()) {
                                    Collection collection = GrailsMetaClassUtils.getPropertyIfExists(obj[propName], otherSide.name, Collection)
                                    if (collection == null || !collection.contains(obj)) {
                                        def methodName = 'addTo' + GrailsNameUtils.getClassName(otherSide.name)
                                        GrailsMetaClassUtils.invokeMethodIfExists(obj[propName], methodName, [obj] as Object[])
                                    }
                                }
                            }
                        })
                    }
                }
            }
        }
        if(!isSet) {
            super.setPropertyValue obj, source, propName, propertyValue, listener
        }
    }

    protected addElementToCollection(obj, String propName, GrailsDomainClassProperty property, propertyValue, boolean clearCollection) {
        boolean isSet = false
        def coll = initializeCollection obj, propName, property.type
        if(coll != null) {
            if(clearCollection) {
                coll.clear()
            }
            def referencedType = getReferencedTypeForCollection propName, obj
            if(referencedType != null) {
                if(isDomainClass(referencedType)) {
                    def persistentInstance = getPersistentInstance referencedType, propertyValue
                    if(persistentInstance != null) {
                        coll << persistentInstance
                        isSet = true
                    }
                } else if(referencedType.isAssignableFrom(propertyValue.getClass())){
                    coll << propertyValue
                    isSet = true
                } else {
                    try {
                        coll << convert(referencedType, propertyValue)
                        isSet = true
                    } catch(Exception e){}
                }
            }
        }
        isSet
    }

    @Autowired(required=false)
    void setValueConverters(ValueConverter[] converters) {
        converters.each { ValueConverter converter ->
            registerConverter converter
        }
    }

    @Autowired(required=false)
    void setFormattedValueConverters(FormattedValueConverter[] converters) {
        converters.each { FormattedValueConverter converter ->
            registerFormattedValueConverter converter
        }
    }

    protected convert(Class typeToConvertTo, value) {
        if (value instanceof JSONObject.Null) {
            return null
        }
        super.convert typeToConvertTo, value
    }
}
