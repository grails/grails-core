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
import grails.validation.ValidationErrors
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.slurpersupport.GPathResult

import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

import org.codehaus.groovy.grails.commons.AnnotationDomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.commons.GrailsMetaClassUtils
import org.codehaus.groovy.grails.web.binding.converters.ByteArrayMultipartFileValueConverter
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.MetaClassHelper
import org.codehaus.groovy.runtime.metaclass.ThreadManagedMetaBeanProperty
import org.grails.databinding.BindingFormat
import org.grails.databinding.ClosureValueConverter
import org.grails.databinding.DataBindingSource
import org.grails.databinding.IndexedPropertyReferenceDescriptor
import org.grails.databinding.SimpleDataBinder
import org.grails.databinding.SimpleMapDataBindingSource
import org.grails.databinding.converters.FormattedValueConverter
import org.grails.databinding.converters.ValueConverter
import org.grails.databinding.errors.BindingError
import org.grails.databinding.events.DataBindingListener
import org.grails.databinding.xml.GPathResultMap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError

@CompileStatic
class GrailsWebDataBinder extends SimpleDataBinder {
    protected static final Map<Class, List> CLASS_TO_BINDING_INCLUDE_LIST = new ConcurrentHashMap<Class, List>()
    protected GrailsApplication grailsApplication
    protected MessageSource messageSource
    boolean trimStrings = true
    boolean convertEmptyStringsToNull = true

    GrailsWebDataBinder(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
        this.conversionService = new SpringConversionServiceAdapter()
        registerConverter new ByteArrayMultipartFileValueConverter()
    }

    /**
     * 
     * @param obj The object being bound to
     * @param source The data binding source
     * @see DataBindingSource
     */
    void bind(obj, DataBindingSource source) {
        bind obj, source, null, getBindingIncludeList(obj), null, null
    }

    /**
     * 
     * @param obj The object being bound to
     * @param source The data binding source
     * @param listener A listener which will be notifed of data binding events triggered
     * by this binding
     * @see DataBindingSource
     * @see DataBindingListener
     */
    void bind(obj, DataBindingSource source, DataBindingListener listener) {
        bind obj, source, null, getBindingIncludeList(obj), null, listener
    }

    /**
     * 
     * @param obj The object being bound to
     * @param source The data binding source
     * @param filter Only properties beginning with filter will be included in the
     * data binding.  For example, if filter is &quot;person&quot; and the binding
     * source contains data for properties &quot;person.name&quot; and &quot;author.name&quot;
     * the value of &quot;person.name&quot; will be bound to obj.name.  The value of
     * &quot;author.name&quot; will be ignored.
     * @param whiteList A list of property names to be included during this 
     * data binding.  All other properties represented in the binding source 
     * will be ignored
     * @param blackList A list of properties names to be excluded during
     * this data binding.  
     * @see DataBindingSource
     * @see DataBindingListener
     */
    void bind(object, DataBindingSource source, String filter, List whiteList, List blackList, DataBindingListener listener) {
        def bindingResult = new BeanPropertyBindingResult(object, object.getClass().name)
        def errorHandlingListener = new GrailsWebDataBindingListener(bindingResult, messageSource)

        def listenerWrapper = new DataBindingEventMulticastListener(listeners: [errorHandlingListener, listener])
        super.bind object, source, filter, whiteList, blackList, listenerWrapper
        populateErrors(object, bindingResult)
    }

    /**
     * 
     * @param obj The object being bound to
     * @param gpath A GPathResult which represents the data being bound.  
     * @see DataBindingSource
     */
    void bind(obj, GPathResult gpath) {
        bind obj, new SimpleMapDataBindingSource(new GPathResultMap(gpath)), getBindingIncludeList(obj)
    }

    protected populateErrors(obj, BindingResult bindingResult) {
        GrailsDomainClass domain = null
        if (grailsApplication != null) {
            domain = (GrailsDomainClass) grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,obj.getClass().name)
        }

        if (domain != null && bindingResult != null) {
            def newResult = new ValidationErrors(obj)
            for (Object error : bindingResult.getAllErrors()) {
                if (error instanceof FieldError) {
                    def fieldError = (FieldError)error
                    final boolean isBlank = ''.equals(fieldError.getRejectedValue())
                    if (!isBlank) {
                        newResult.addError(fieldError)
                    }
                    else if (domain.hasPersistentProperty(fieldError.getField())) {
                        final boolean isOptional = domain.getPropertyByName(fieldError.getField()).isOptional()
                        if (!isOptional) {
                            newResult.addError(fieldError)
                        }
                    }
                    else {
                        newResult.addError(fieldError)
                    }
                }
                else {
                    newResult.addError((ObjectError)error)
                }
            }
            bindingResult = newResult
        }
        def mc = GroovySystem.getMetaClassRegistry().getMetaClass(obj.getClass())
        if (mc.hasProperty(obj, "errors")!=null && bindingResult!=null) {
            def errors = new ValidationErrors(obj)
            errors.addAllErrors(bindingResult)
            mc.setProperty(obj,"errors", errors)
        }
    }


    @Override
    protected Class<?> getReferencedTypeForCollection(String name, Object target) {
        def referencedType
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
        if(source.dataSourceAware) {
            def isDomainClass = isDomainClass propertyType
            if (isDomainClass && source.containsProperty(propName)) {
                def val = source.getPropertyValue propName
                def idValue = getIdentifierValueFrom(val)
                if (idValue != null) {
                    def persistentInstance = getPersistentInstance(propertyType, idValue)
                    if (persistentInstance != null) {
                        obj[propName] = persistentInstance
                        isInitialized = true
                    }
                }
            }
        }
        if (!isInitialized) {
            super.initializeProperty obj, propName,  propertyType, source
        }
    }

    protected getPersistentInstance(Class<?> type, id) {
        try {
            InvokerHelper.invokeStaticMethod type, 'get', id
        } catch (Exception exc) {}
    }

    /**
     * @param obj any object
     * @param propName the name of a property on obj
     * @return the Class of the domain class referenced by propName, null if propName does not reference a domain class
     */
    protected Class getDomainClassType(obj, String propName) {
        def domainClassType
        def objClass = obj.getClass()
        def propertyType = GrailsClassUtils.getPropertyType(objClass, propName)
        if(propertyType && isDomainClass(propertyType)) {
            domainClassType = propertyType
        }
        domainClassType
    }

    protected boolean isDomainClass(final Class<?> clazz) {
        return DomainClassArtefactHandler.isDomainClass(clazz) || AnnotationDomainClassArtefactHandler.isJPADomainClass(clazz)
    }

    protected getIdentifierValueFrom(source) {
        def idValue = null
        if(source instanceof DataBindingSource && source.hasIdentifier()) {
            idValue = source.getIdentifierValue()
        } else if(source instanceof CharSequence){
            idValue = source
        } else if(source instanceof Map && source.containsKey('id')) {
            idValue = source['id']
        } else if(source instanceof Number) {
            idValue = source.toString()
        }
        if (idValue instanceof GString) {
            idValue = idValue.toString()
        }
        idValue
    }

    @Override
    protected processProperty(obj, MetaProperty metaProperty, val, DataBindingSource source, DataBindingListener listener) {
        boolean needsBinding = true

        if (source.dataSourceAware) {
            def propName = metaProperty.name
            def propertyType = getDomainClassType(obj, metaProperty.name)
            if (propertyType && isDomainClass(propertyType)) {
                def idValue = getIdentifierValueFrom(val)
                if (idValue != 'null' && idValue != null && idValue != '') {
                    def persistedInstance = getPersistentInstance(propertyType, idValue)
                    if (persistedInstance != null) {
                        needsBinding = false
                        bindProperty obj, source, metaProperty, persistedInstance, listener
                        if (persistedInstance != null) {
                            if (val instanceof Map) {
                                bind persistedInstance, new SimpleMapDataBindingSource(val), listener
                            } else if (val instanceof DataBindingSource) {
                                bind persistedInstance, val, listener
                            }
                        }
                    }
                } else {
                    boolean shouldBindNull = false
                    if(val instanceof DataBindingSource) {
                        // bind null if this binding source does contain an identifier
                        shouldBindNull = ((DataBindingSource)val).hasIdentifier()
                    } else if(val instanceof Map) {
                        // bind null if this Map does contain an id
                        shouldBindNull = ((Map)val).containsKey('id')
                    } else if(idValue instanceof CharSequence) {
                        // bind null if idValue is a CharSequence because it would have
                        // to be 'null' or '' in order for control to be in this else block
                        shouldBindNull = true
                    }
                    if(shouldBindNull) {
                        needsBinding = false
                        bindProperty obj, source, metaProperty, null, listener
                    }
                }
            } else if(Collection.isAssignableFrom(metaProperty.type)) {
                def referencedType = getReferencedTypeForCollection(propName, obj)
                if(referencedType && isDomainClass(referencedType) && val instanceof List) {
                    needsBinding = false
                    def itemsWhichNeedBinding = []
                    val.each { item ->
                        if(item instanceof Map || item instanceof DataBindingSource) {
                            def idValue = getIdentifierValueFrom(item)
                            def persistentInstance
                            if(idValue != null) {
                                persistentInstance = getPersistentInstance(referencedType, idValue)
                                if(persistentInstance != null) {
                                    bind persistentInstance, new SimpleMapDataBindingSource(item), listener
                                }
                            }
                            if(persistentInstance == null) {
                                itemsWhichNeedBinding << item
                            }
                        } else {
                            itemsWhichNeedBinding << item
                        }
                    }
                    if(itemsWhichNeedBinding) {
                        for(item in itemsWhichNeedBinding) {
                            addElementToCollection obj, metaProperty.name, metaProperty.type, item, false
                        }
                    }
                }
            }
        }
        if (needsBinding) {
            super.processProperty obj, metaProperty, val, source, listener
        }
    }

    @Override
    protected processIndexedProperty(obj, MetaProperty metaProperty, IndexedPropertyReferenceDescriptor indexedPropertyReferenceDescriptor, val,
            DataBindingSource source, DataBindingListener listener) {

        boolean needsBinding = true
        if (source.dataSourceAware) {
            def propName = indexedPropertyReferenceDescriptor.propertyName

            def idValue = getIdentifierValueFrom(val)
            if (idValue != null) {
                def propertyType = getDomainClassType(obj, propName)
                def referencedType = getReferencedTypeForCollection propName, obj
                if (referencedType != null && isDomainClass(referencedType)) {
                    needsBinding = false
                    if (Set.isAssignableFrom(metaProperty.type)) {
                        def collection = initializeCollection obj, propName, metaProperty.type
                        def instance
                        if (collection != null) {
                            instance = findAlementWithId((Set)collection, idValue)
                        }
                        if (instance == null) {
                            if ('null' != idValue) {
                                instance = getPersistentInstance(referencedType, idValue)
                            }
                            if (instance == null) {
                                def message = "Illegal attempt to update element in [${propName}] Set with id [${idValue}]. No such record was found."
                                Exception e = new IllegalArgumentException(message)
                                addBindingError(obj, propName, idValue, e, listener)
                            } else {
                                addElementToCollectionAt obj, propName, collection, Integer.parseInt(indexedPropertyReferenceDescriptor.index), instance
                            }
                        }
                        if (instance != null) {
                            if (val instanceof Map) {
                                bind instance, new SimpleMapDataBindingSource(val), listener
                            } else if (val instanceof DataBindingSource) {
                                bind instance, val, listener
                            }
                        }
                    } else if (Collection.isAssignableFrom(metaProperty.type)) {
                        def collection = initializeCollection obj, propName, metaProperty.type
                        def idx = Integer.parseInt(indexedPropertyReferenceDescriptor.index)
                        if('null' == idValue) {
                            if(idx < collection.size()) {
                                def element = collection[idx]
                                if(element != null) {
                                    collection.remove element
                                }
                            }
                        } else {
                            def instance = getPersistentInstance(referencedType, idValue)
                            addElementToCollectionAt obj, propName, collection, idx, instance
                            if (instance != null) {
                                if (val instanceof Map) {
                                    bind instance, new SimpleMapDataBindingSource(val), listener
                                } else if (val instanceof DataBindingSource) {
                                    bind instance, val, listener
                                }
                            }
                        }
                    } else if (Map.isAssignableFrom(metaProperty.type)) {
                        Map map = (Map)obj[propName]
                        if (idValue == 'null' || idValue == null || idValue == '') {
                            if (map != null) {
                                map.remove indexedPropertyReferenceDescriptor.index
                            }
                        } else {
                            map = initializeMap obj, propName
                            def persistedInstance = getPersistentInstance referencedType, idValue
                            if (persistedInstance != null) {
                                if (map.size() < autoGrowCollectionLimit || map.containsKey(indexedPropertyReferenceDescriptor.index)) {
                                    map[indexedPropertyReferenceDescriptor.index] = persistedInstance
                                    if (val instanceof Map) {
                                        bind persistedInstance, new SimpleMapDataBindingSource(val), listener
                                    } else if (val instanceof DataBindingSource) {
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
        }
        if (needsBinding) {
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
        if (domainClass != null) {
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
        if (grailsApplication != null) {
            def domainClass = (GrailsDomainClass)grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, obj.getClass().name)
            if (domainClass != null) {
                def property = domainClass.getPersistentProperty propName
                if (property != null) {
                    if (Collection.isAssignableFrom(property.type)) {
                        if (propertyValue instanceof String) {
                            isSet = addElementToCollection obj, propName, property, propertyValue, true
                        } else if (propertyValue instanceof String[]){
                            if (isDomainClass(property.referencedPropertyType)) {
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
        if (!isSet) {
            super.setPropertyValue obj, source, metaProperty, propertyValue, listener
        }
    }

    protected preprocessCharSequenceValue(CharSequence propertyValue) {
        String stringValue = propertyValue.toString()
        if (trimStrings) {
            stringValue = stringValue.trim()
        }
        if (convertEmptyStringsToNull && "".equals(stringValue)) {
            stringValue = null
        }
        return stringValue
    }
    
    protected addElementToCollection(obj, String propName, Class propertyType, propertyValue, boolean clearCollection) {
        boolean isSet = false
        def coll = initializeCollection obj, propName, propertyType
        if (coll != null) {
            if (clearCollection) {
                coll.clear()
            }
            def referencedType = getReferencedTypeForCollection propName, obj
            if (referencedType != null) {
                if (isDomainClass(referencedType)) {
                    def persistentInstance = getPersistentInstance referencedType, propertyValue
                    if (persistentInstance != null) {
                        coll << persistentInstance
                        isSet = true
                    } else {
                    try {
                        coll << convert(referencedType, propertyValue)
                        isSet = true
                    } catch(Exception e){}
                    }
                } else if (referencedType.isAssignableFrom(propertyValue.getClass())){
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

    protected addElementToCollection(obj, String propName, GrailsDomainClassProperty property, propertyValue, boolean clearCollection) {
        addElementToCollection obj, propName, property.type, propertyValue, clearCollection
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
        if (!converter && propValue instanceof CharSequence) {
            Closure closure = { source ->
                preprocessCharSequenceValue propValue
            }
            converter = new ClosureValueConverter(converterClosure: closure, targetType: String)
        }
        converter
    }

    @Autowired
    void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource
    }

    @Override
    protected String getFormatString(BindingFormat annotation) {
        def formatString
        def code = annotation.code()
        if(code) {
            def locale = getLocale()
            formatString = messageSource.getMessage(code, [] as Object[], locale)
        }
        if(!formatString) {
            formatString = super.getFormatString(annotation)
        }
        formatString
    }

    protected Locale getLocale() {
        def request = GrailsWebRequest.lookup()
        request ? request.getLocale() : Locale.getDefault()
    }
}

@CompileStatic
class DataBindingEventMulticastListener implements DataBindingListener {
    List<DataBindingListener> listeners

    Boolean beforeBinding(Object obj, String propertyName, Object value) {
        listeners*.beforeBinding obj, propertyName, value
    }

    void afterBinding(Object obj, String propertyName) {
        listeners*.afterBinding obj, propertyName
    }

    void bindingError(BindingError error) {
        listeners*.bindingError error
    }
}
