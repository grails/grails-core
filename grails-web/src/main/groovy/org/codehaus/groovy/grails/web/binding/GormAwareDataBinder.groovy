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
import groovy.transform.TypeCheckingMode
import groovy.util.slurpersupport.GPathResult

import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

import org.codehaus.groovy.grails.commons.AnnotationDomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.commons.GrailsMetaClassUtils
import org.codehaus.groovy.grails.web.binding.converters.ByteArrayMultipartFileValueConverter
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.MetaClassHelper
import org.codehaus.groovy.runtime.metaclass.ThreadManagedMetaBeanProperty
import org.grails.databinding.ClosureValueConverter
import org.grails.databinding.DataBindingSource
import org.grails.databinding.IndexedPropertyReferenceDescriptor
import org.grails.databinding.SimpleDataBinder
import org.grails.databinding.SimpleMapDataBindingSource
import org.grails.databinding.converters.FormattedValueConverter
import org.grails.databinding.converters.ValueConverter
import org.grails.databinding.events.DataBindingListener
import org.grails.databinding.xml.GPathResultMap
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
class GormAwareDataBinder extends SimpleDataBinder {
    protected static final Map<Class, List> CLASS_TO_BINDING_INCLUDE_LIST = new ConcurrentHashMap<Class, List>()
    protected GrailsApplication grailsApplication
    boolean trimStrings = true
    boolean convertEmptyStringsToNull = true

    GormAwareDataBinder(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
        this.conversionService = new SpringConversionServiceAdapter()
        registerConverter new ByteArrayMultipartFileValueConverter()
    }

    /**
     * @param obj the object to perform data binding on
     * @param source a Map containg the values to be bound to obj
     */
    void bind(obj, DataBindingSource source) {
        bind obj, source, null, getBindingIncludeList(obj), null, null
    }

    /**
     * @param obj the object to perform data binding on
     * @param source a Map containg the values to be bound to obj
     * @param listener will be notified of data binding events
     */
    void bind(obj, DataBindingSource source, DataBindingListener listener) {
        bind obj, source, null, getBindingIncludeList(obj), null, listener
    }

    /**
     * @param obj the object to perform data binding on
     * @param gpath contains an XML representation of the data to be bound to obj
     */
    void bind(obj, GPathResult gpath) {
        bind obj, new SimpleMapDataBindingSource(new GPathResultMap(gpath)), getBindingIncludeList(obj)
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
    protected initializeProperty(obj, String propName, Class propertyType, DataBindingSource source) {
        def isInitialized = false
        def isDomainClass = isDomainClass propertyType
        if(isDomainClass && source.containsProperty(propName)) {
            def val = source.getPropertyValue propName
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
        def persistentInstace = null
        try {
            persistentInstace = InvokerHelper.invokeStaticMethod type, 'get', id
        } catch (Exception exc) {}
        persistentInstace
    }
    
    /**
     * @param obj any object
     * @param propName the name of a property on obj
     * @return the Class of the domain class referenced by propName, null if propName does not reference a domain class
     */
    protected Class getDomainClassType(obj, String propName) {
        def domainClassType = null
        def objClass = obj.getClass()
        if(grailsApplication) {
            def domainClass = (GrailsDomainClass)grailsApplication.getArtefact('Domain', objClass.name)
            if(domainClass) {
                def prop = domainClass.getPersistentProperty(propName)
                if(prop && isDomainClass(prop.type)) {
                    domainClassType = prop.type
                }
            }
        }
        domainClassType
    }
    
    protected boolean isDomainClass(final Class<?> clazz) {
        return DomainClassArtefactHandler.isDomainClass(clazz) || AnnotationDomainClassArtefactHandler.isJPADomainClass(clazz)
    }

    @Override
    protected processProperty(obj, MetaProperty metaProperty, val, DataBindingSource source, DataBindingListener listener) {
        boolean needsBinding = true
        
        def propName = metaProperty.name
        if((val instanceof Map && val.containsKey('id')) || (val instanceof CharSequence)) {
            def idValue = val instanceof Map ? val['id'] : val
            if(idValue instanceof GString) {
                idValue = idValue.toString()
            }
            def propertyType = getDomainClassType(obj, metaProperty.name)
            if(propertyType) {
                needsBinding = false
                def persistedInstance = null
                if(idValue != 'null' && idValue != null && idValue != '') {
                    persistedInstance = getPersistentInstance(propertyType, idValue)
                    if(persistedInstance == null) {
                        needsBinding = true
                    } else {
                        bindProperty obj, source, metaProperty, persistedInstance, listener
                        if(persistedInstance != null) {
                            if(val instanceof Map) {
                                bind persistedInstance, new SimpleMapDataBindingSource(val), listener
                            } else if(val instanceof DataBindingSource) {
                                bind persistedInstance, val, listener
                            }
                        }
                    }
                } else {
                    bindProperty obj, source, metaProperty, null, listener
                }
            }
        }
        if(needsBinding) {
            super.processProperty obj, metaProperty, val, source, listener
        }
    }

    @Override    
    protected processIndexedProperty(obj, MetaProperty metaProperty, IndexedPropertyReferenceDescriptor indexedPropertyReferenceDescriptor, val, DataBindingSource source, DataBindingListener listener) { 
        boolean needsBinding = true
        def propName = indexedPropertyReferenceDescriptor.propertyName
        
        if((val instanceof Map && val.containsKey('id')) || (val instanceof CharSequence)) {
            def idValue = val instanceof Map ? val['id'] : val
            if(idValue instanceof GString) {
                idValue = idValue.toString()
            }
            def propertyType = getDomainClassType(obj, propName)
            def referencedType = getReferencedTypeForCollection propName, obj
            if(referencedType != null && isDomainClass(referencedType)) {
                needsBinding = false
                if(Set.isAssignableFrom(metaProperty.type)) {
                    def collection = initializeCollection obj, propName, metaProperty.type
                    def instance
                    if(collection != null) {
                        instance = findAlementWithId((Set)collection, idValue)
                    }
                    if(instance == null) {
                        if('null' != idValue) {
                            instance = getPersistentInstance(referencedType, idValue)
                        }
                        if(instance == null) {
                            def message = "Illegal attempt to update element in [${propName}] Set with id [${idValue}]. No such record was found."
                            Exception e = new IllegalArgumentException(message)
                            addBindingError(obj, propName, idValue, e, listener)
                        } else {
                            addElementToCollectionAt obj, propName, collection, Integer.parseInt(indexedPropertyReferenceDescriptor.index), instance
                        }
                    }
                    if(instance != null) {
                        if(val instanceof Map) {
                            bind instance, new SimpleMapDataBindingSource(val), listener
                        } else if(val instanceof DataBindingSource) {
                            bind instance, val, listener
                        }
                    } 
                } else if(Collection.isAssignableFrom(metaProperty.type)) {
                    def instance = 'null' == idValue ? null : getPersistentInstance(referencedType, idValue)
                    def collection = initializeCollection obj, propName, metaProperty.type
                    addElementToCollectionAt obj, propName, collection, Integer.parseInt(indexedPropertyReferenceDescriptor.index), instance
                    if(instance != null) {
                        if(val instanceof Map) {
                            bind instance, new SimpleMapDataBindingSource(val), listener
                        } else if(val instanceof DataBindingSource) {
                            bind instance, val, listener
                        }
                    } 
                } else if(Map.isAssignableFrom(metaProperty.type)) {
                    Map map = (Map)obj[propName]
                    if(idValue == 'null' || idValue == null || idValue == '') {
                        if(map != null) {
                            map.remove indexedPropertyReferenceDescriptor.index
                        }
                    } else {
                        map = initializeMap obj, propName
                        def persistedInstance = getPersistentInstance referencedType, idValue
                        if(persistedInstance != null) {
                            if(map.size() < autoGrowCollectionLimit || map.containsKey(indexedPropertyReferenceDescriptor.index)) {
                                map[indexedPropertyReferenceDescriptor.index] = persistedInstance
                                if(val instanceof Map) {
                                    bind persistedInstance, new SimpleMapDataBindingSource(val), listener
                                } else if(val instanceof DataBindingSource) {
                                    bind persistedInstance, val, listener
                                }
                            }
                        } else {
                            map.remove indexedPropertyReferenceDescriptor.index
                        }
                    }
                }
            }
        }
        if(needsBinding) {
            super.processIndexedProperty obj, metaProperty, indexedPropertyReferenceDescriptor, val, source, listener
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private findAlementWithId(Set set,  idValue) {
        set.find {
            it.id == idValue
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
    private Map resolveConstrainedProperties(object) {
        Map constrainedProperties = null
        MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(object.getClass())
        MetaProperty metaProp = mc.getMetaProperty('constraints')
        if (metaProp != null) {
            Object constrainedPropsObj = getMetaPropertyValue(metaProp, object)
            if (constrainedPropsObj instanceof Map) {
                constrainedProperties = (Map)constrainedPropsObj
            }
        }
        constrainedProperties
    }
    private getMetaPropertyValue(MetaProperty metaProperty, delegate) {
        if (metaProperty instanceof ThreadManagedMetaBeanProperty) {
            return ((ThreadManagedMetaBeanProperty)metaProperty).getGetter().invoke(delegate, MetaClassHelper.EMPTY_ARRAY)
        }

        return metaProperty.getProperty(delegate)
    }

    @Override
    protected setPropertyValue(obj, DataBindingSource source, MetaProperty metaProperty, propertyValue, DataBindingListener listener) {
        def propName = metaProperty.name
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
            super.setPropertyValue obj, source, metaProperty, propertyValue, listener
        }
    }

    protected preprocessCharSequenceValue(CharSequence propertyValue) {
        String stringValue = propertyValue.toString()
        if(trimStrings) {
            stringValue = stringValue.trim()
        }
        if (convertEmptyStringsToNull && "".equals(stringValue)) {
            stringValue = null
        }
        return stringValue
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
    
    @Override
    protected ValueConverter getValueConverter(obj, String propName, propValue) {
        def converter = super.getValueConverter obj, propName, propValue
        if(!converter && propValue instanceof CharSequence) {
            Closure closure = { source ->
                preprocessCharSequenceValue propValue
            }
            converter = new ClosureValueConverter(converterClosure: closure, targetType: String)
        }
        converter
    }
}


